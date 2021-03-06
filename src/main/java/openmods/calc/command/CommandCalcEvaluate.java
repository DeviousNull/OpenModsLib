package openmods.calc.command;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import openmods.calc.Calculator.ExprType;

public class CommandCalcEvaluate extends CommandCalcBase {
	private static final String NAME = "=eval";

	public CommandCalcEvaluate(CalcState state) {
		super(NAME, state);
	}

	@Override
	public String getCommandUsage(ICommandSender p_71518_1_) {
		return NAME + " expr";
	}

	@Override
	public List<?> getCommandAliases() {
		return Lists.newArrayList("=evaluate", "=");
	}

	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		final String expr = spaceJoiner.join(args);
		try {
			if (state.exprType == ExprType.INFIX) {
				final String result = state.compileExecuteAndPrint(sender, expr);
				sender.addChatMessage(new ChatComponentText(result));
			} else {
				state.compileAndExecute(sender, expr);
				sender.addChatMessage(new ChatComponentTranslation("openmodslib.command.calc_stack_size", state.getActiveCalculator().stackSize()));
			}
		} catch (Exception e) {
			final List<String> causes = Lists.newArrayList();
			Throwable current = e;
			while (current != null) {
				causes.add(Strings.nullToEmpty(current.getMessage()));
				current = current.getCause();
			}
			throw new CommandException("openmodslib.command.calc_error", Joiner.on("', caused by '").join(causes));
		}
	}

}
