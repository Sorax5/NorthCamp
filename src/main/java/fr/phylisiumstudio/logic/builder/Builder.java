package fr.phylisiumstudio.logic.builder;

import java.util.concurrent.CompletableFuture;

public interface Builder<T, U, W> {
    CompletableFuture<Void> Build(T data, U state, W instance);
}
