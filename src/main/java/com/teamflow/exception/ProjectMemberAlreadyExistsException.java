package com.teamflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProjectMemberAlreadyExistsException extends RuntimeException {

    public ProjectMemberAlreadyExistsException() {
        super("The user is already a member of this project.");
    }
}
