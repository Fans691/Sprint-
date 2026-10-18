package mg.itu.framework.model;

public class MapFile {
    private String fileName;
    private byte[] data;
    private String extension;

    public MapFile() {
    }

    public MapFile(String fileName, byte[] data, String extension) {
        this.fileName = fileName;
        this.data = data;
        this.extension = extension;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }
}
