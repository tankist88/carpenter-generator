package org.carpenter.generator.command;

import java.util.List;

public interface ReturnCommand<T> extends Command {
    List<T> returnResult();
}
