package com.github.tankist88.carpenter.generator.command;

import java.util.List;

public interface ReturnCommand<T> extends Command {
    List<T> returnResult();
}
