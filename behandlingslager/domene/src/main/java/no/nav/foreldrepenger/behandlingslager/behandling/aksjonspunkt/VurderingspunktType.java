package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

public enum VurderingspunktType {
    INN("INN"), //$NON-NLS-1$
    UT("UT"); //$NON-NLS-1$

    private final String dbKode;

    private VurderingspunktType(String dbKode) {
        this.dbKode = dbKode;
    }

    public String getDbKode() {
        return dbKode;
    }

    public static VurderingspunktType getType(String kode) {
        switch (kode) {
            case "INN": //$NON-NLS-1$
                return INN;
            case "UT": //$NON-NLS-1$
                return UT;
            default:
                throw new IllegalArgumentException("Ukjent kode: " + kode); //$NON-NLS-1$
        }
    }
}