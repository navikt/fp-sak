package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

public enum VurderingspunktType {
    INN("INN"), //$NON-NLS-1$
    UT("UT"); //$NON-NLS-1$

    private final String dbKode;

    VurderingspunktType(String dbKode) {
        this.dbKode = dbKode;
    }

    public String getDbKode() {
        return dbKode;
    }

    public static VurderingspunktType getType(String kode) {
        return switch (kode) {
            case "INN" -> INN;
            case "UT" -> UT;
            default -> throw new IllegalArgumentException("Ukjent kode: " + kode); //$NON-NLS-1$
        };
    }
}
