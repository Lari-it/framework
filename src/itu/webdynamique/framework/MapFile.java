package itu.webdynamique.framework;

public class MapFile {
    private String nom;
    private String extension;
    private byte[] binaire;

    public MapFile() {
    }

    public MapFile(String nom, String extension, byte[] binaire) {
        this.nom = nom;
        this.extension = extension;
        this.binaire = binaire;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public byte[] getBinaire() {
        return binaire;
    }

    public void setBinaire(byte[] binaire) {
        this.binaire = binaire;
    }

    @Override
    public String toString() {
        return "MapFile{" +
                "nom='" + nom + '\'' +
                ", extension='" + extension + '\'' +
                ", taille=" + (binaire != null ? binaire.length : 0) + " bytes" +
                '}';
    }
}
