package openmods.network.event;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.io.*;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.network.INetHandler;
import openmods.OpenMods;
import openmods.utils.io.PacketChunker;

import com.google.common.base.Preconditions;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;

@Sharable
public class EventPacketCodec extends MessageToMessageCodec<FMLProxyPacket, EventPacket> {

	private final PacketChunker chunker = new PacketChunker();

	private final EventPacketRegistry registry;

	public EventPacketCodec(EventPacketRegistry registry) {
		this.registry = registry;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, EventPacket msg, List<Object> out) throws Exception {
		IEventPacketType type = registry.getTypeForClass(msg.getClass());

		byte[] payload = toRawBytes(msg, type.isCompressed());
		Channel channel = ctx.channel();
		String channelName = channel.attr(NetworkRegistry.FML_CHANNEL).get();

		Side side = channel.attr(NetworkRegistry.CHANNEL_SOURCE).get();
		PacketDirectionValidator validator = type.getDirection();
		Preconditions.checkState(validator != null && validator.validateSend(side),
				"Invalid direction: sending packet %s on side %s", msg.getClass(), side);

		if (type.isChunked()) {
			final int maxChunkSize = side == Side.SERVER? PacketChunker.PACKET_SIZE_S3F : PacketChunker.PACKET_SIZE_C17;
			byte[][] chunked = chunker.splitIntoChunks(payload, maxChunkSize);
			for (byte[] chunk : chunked) {
				FMLProxyPacket partialPacket = createPacket(type, chunk, channelName);
				partialPacket.setDispatcher(msg.dispatcher);
				out.add(partialPacket);
			}
		} else {
			FMLProxyPacket partialPacket = createPacket(type, payload, channelName);
			partialPacket.setDispatcher(msg.dispatcher);
			out.add(partialPacket);
		}
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, FMLProxyPacket msg, List<Object> out) throws Exception {
		ByteBuf payload = msg.payload();
		int typeId = ByteBufUtils.readVarInt(payload, 5);
		IEventPacketType type = registry.getTypeForId(typeId);

		Channel channel = ctx.channel();

		Side side = channel.attr(NetworkRegistry.CHANNEL_SOURCE).get();

		PacketDirectionValidator validator = type.getDirection();
		Preconditions.checkState(validator != null && validator.validateReceive(side),
				"Invalid direction: receiving packet %s on side %s", msg.getClass(), side);

		InputStream input;

		byte[] bytes = new byte[payload.readableBytes()];
		payload.readBytes(bytes);

		if (type.isChunked()) {
			byte[] fullPayload = chunker.consumeChunk(bytes);
			if (fullPayload == null) return;
			input = new ByteArrayInputStream(fullPayload);
		} else {
			input = new ByteArrayInputStream(bytes);
		}

		if (type.isCompressed()) input = new GZIPInputStream(input);

		DataInput data = new DataInputStream(input);

		EventPacket event = type.createPacket();
		event.readFromStream(data);
		event.dispatcher = msg.getDispatcher();

		INetHandler handler = msg.handler();
		if (handler != null) event.sender = OpenMods.proxy.getPlayerFromHandler(handler);
		input.close();

		out.add(event);
	}

	private static FMLProxyPacket createPacket(IEventPacketType type, byte[] payload, String channel) {
		ByteBuf buf = Unpooled.buffer(payload.length + 5);
		ByteBufUtils.writeVarInt(buf, type.getId(), 5);
		buf.writeBytes(payload);
		FMLProxyPacket partialPacket = new FMLProxyPacket(buf.copy(), channel);
		return partialPacket;
	}

	private static byte[] toRawBytes(EventPacket event, boolean compress) throws IOException {
		ByteArrayOutputStream payload = new ByteArrayOutputStream();

		OutputStream stream = compress? new GZIPOutputStream(payload) : payload;
		DataOutputStream output = new DataOutputStream(stream);
		event.writeToStream(output);
		stream.close();

		return payload.toByteArray();
	}
}
