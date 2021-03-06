package com.intellij.perlplugin.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.perlplugin.ModulesContainer;
import com.intellij.perlplugin.Utils;
import com.intellij.perlplugin.extensions.module.builder.PerlModuleType;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by eli on 27-11-14.
 */
public class ApplicationComponent implements com.intellij.openapi.components.ApplicationComponent, BulkFileListener {

    private final MessageBusConnection connection;

    public ApplicationComponent() {
        ModuleTypeManager.getInstance().registerModuleType(new PerlModuleType());//Make sure we register our module type to avoid: Unknown Module Type - Cannot determine module type for the following modules:
        connection = ApplicationManager.getApplication().getMessageBus().connect();
    }

    public void initComponent() {
        connection.subscribe(VirtualFileManager.VFS_CHANGES, this);
    }

    public void disposeComponent() {
        connection.disconnect();
    }

    @NotNull
    public String getComponentName() {
        return "ApplicationComponent";
    }

    @Override
    public void before(@NotNull List<? extends VFileEvent> list) {
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> list) {
        try {
            VFileEvent fileEvent;
            for (int i = 0; i < list.size(); i++) {
                fileEvent = list.get(i);
                if (fileEvent instanceof VFileMoveEvent) {
                    VFileMoveEvent vFile = (VFileMoveEvent) fileEvent;
                    if (!vFile.getOldPath().equals(vFile.getPath())) {
                        ModulesContainer.renameFile(vFile.getOldPath(), vFile.getPath());
                    }
                } else if (fileEvent instanceof VFilePropertyChangeEvent) {
                    VFilePropertyChangeEvent vFile = (VFilePropertyChangeEvent) fileEvent;
                    if (!vFile.getOldPath().equals(vFile.getPath())) {
                        ModulesContainer.renameFile(vFile.getOldPath(), vFile.getPath());
                    }
                } else if (fileEvent instanceof VFileDeleteEvent) {
                    VFileDeleteEvent vFile = (VFileDeleteEvent) fileEvent;
                    ModulesContainer.deleteFile(vFile.getPath());
                } else if (fileEvent instanceof VFileCreateEvent) {
                    VFileCreateEvent vFile = (VFileCreateEvent) fileEvent;
                    ModulesContainer.createFile(vFile.getPath(), null);
                } if (fileEvent instanceof VFileContentChangeEvent) {
                    VFileContentChangeEvent vFile = (VFileContentChangeEvent) fileEvent;
                    if (Utils.isValidateExtension(vFile.getPath())) {
                        Project project = ProjectManager.getInstance().getOpenProjects()[0];
                        String fileContent = FileEditorManager.getInstance(project).getSelectedTextEditor().getDocument().getText();
                        ModulesContainer.updateFile(vFile.getPath(), fileContent);
                    }
                }

            }
        } catch (Exception e) {
            //if we get an exception we don't want to mess up the listener - (file listener goes into infinite loop)
            e.printStackTrace();
        }
    }
}
