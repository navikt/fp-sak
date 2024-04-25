package no.nav.foreldrepenger.tilganger;

import java.time.LocalDateTime;

public record InnloggetNavAnsattDto(String brukernavn,
                                    String navn,
                                    boolean kanSaksbehandle,
                                    boolean kanVeilede,
                                    boolean kanBeslutte,
                                    boolean kanOverstyre,
                                    boolean kanOppgavestyre,
                                    boolean kanBehandleKodeEgenAnsatt,
                                    boolean kanBehandleKode6,
                                    boolean kanBehandleKode7,
                                    LocalDateTime funksjonellTid) {
    private InnloggetNavAnsattDto(Builder builder) {
        this(builder.brukernavn, builder.navn, builder.kanSaksbehandle, builder.kanVeilede, builder.kanBeslutte, builder.kanOverstyre,
            builder.kanOppgavestyre, builder.kanBehandleKodeEgenAnsatt, builder.kanBehandleKode6, builder.kanBehandleKode7, LocalDateTime.now());
    }

    @Override
    public String toString() {
        return "InnloggetNavAnsattDto{" +
            "kanSaksbehandle=" + kanSaksbehandle +
            ", kanVeilede=" + kanVeilede +
            ", kanBeslutte=" + kanBeslutte +
            ", kanOverstyre=" + kanOverstyre +
            ", kanOppgavestyre=" + kanOppgavestyre +
            ", funksjonellTid=" + funksjonellTid + '}';
    }

    public static class Builder {
        private final String brukernavn;
        private final String navn;
        private boolean kanSaksbehandle;
        private boolean kanVeilede;
        private boolean kanBeslutte;
        private boolean kanOverstyre;
        private boolean kanOppgavestyre;
        private boolean kanBehandleKodeEgenAnsatt;
        private boolean kanBehandleKode6;
        private boolean kanBehandleKode7;

        public Builder(String brukernavn, String navn) {
            this.brukernavn = brukernavn;
            this.navn = navn;
        }

        public Builder kanSaksbehandle(boolean kanSaksbehandle) {
            this.kanSaksbehandle = kanSaksbehandle;
            return this;
        }

        public Builder kanVeilede(boolean kanVeilede) {
            this.kanVeilede = kanVeilede;
            return this;
        }

        public Builder kanBeslutte(boolean kanBeslutte) {
            this.kanBeslutte = kanBeslutte;
            return this;
        }

        public Builder kanOverstyre(boolean kanOverstyre) {
            this.kanOverstyre = kanOverstyre;
            return this;
        }

        public Builder kanOppgavestyre(boolean kanOppgavestyre) {
            this.kanOppgavestyre = kanOppgavestyre;
            return this;
        }

        public Builder kanBehandleKodeEgenAnsatt(boolean kanBehandleKodeEgenAnsatt) {
            this.kanBehandleKodeEgenAnsatt = kanBehandleKodeEgenAnsatt;
            return this;
        }

        public Builder kanBehandleKode6(boolean kanBehandleKode6) {
            this.kanBehandleKode6 = kanBehandleKode6;
            return this;
        }

        public Builder kanBehandleKode7(boolean kanBehandleKode7) {
            this.kanBehandleKode7 = kanBehandleKode7;
            return this;
        }

        public InnloggetNavAnsattDto build() {
            return new InnloggetNavAnsattDto(this);
        }
    }
}
