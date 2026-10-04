package com.teamflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProjectMemberNotFoundException extends RuntimeException {

    public ProjectMemberNotFoundException() {
        super("Project member not found.");
    }
}
