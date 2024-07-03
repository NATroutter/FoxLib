package fi.natroutter.foxlib.data;

public record FileResponse(boolean success, String fileName, String message, String fileContent){}
