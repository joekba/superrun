package com.joeabk.superrun;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(name = "SuperRunSettings", storages = @Storage("superrun.xml"))
public class SuperRunSettings implements PersistentStateComponent<SuperRunSettings.State> {
    private State state = new State();

    public static SuperRunSettings getInstance(Project project) {
        return project.getService(SuperRunSettings.class);
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public static class State {
        public List<String> configurationOrder = new ArrayList<>();
    }
}