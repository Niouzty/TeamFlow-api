package com.teamflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProjectMemberHasAssignedTasksException extends RuntimeException {

    public ProjectMemberHasAssignedTasksException() {
        super("The member cannot be removed while tasks are assigned to them.");
    }
}
