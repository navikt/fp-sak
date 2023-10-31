package no.nav.foreldrepenger.datavarehus.tjeneste;


import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

class CommonDvhMapper {
    private CommonDvhMapper() {
        // hidden
    }

    static String finnEndretAvEllerOpprettetAv(BaseEntitet base) {
        return base.getEndretAv() == null ? base.getOpprettetAv() : base.getEndretAv();
    }

    static boolean erSaksbehandler(String s) {
        return s != null && !s.startsWith("srv") && !s.startsWith("SRV") && !"VL".equals(s);
    }
}
