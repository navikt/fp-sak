package no.nav.foreldrepenger.tilganger;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

public record InnloggetNavAnsattDto(@NotNull String brukernavn,
                                    @NotNull String navn,
                                    @NotNull boolean kanSaksbehandle,
                                    @NotNull boolean kanVeilede,
                                    @NotNull boolean kanOverstyre,
                                    @NotNull boolean kanOppgavestyre,
                                    @NotNull boolean kanBehandleKode6,
                                    LocalDateTime funksjonellTid) {

    private InnloggetNavAnsattDto(Builder builder) {
        this(builder.brukernavn, builder.navn, builder.kanSaksbehandle, builder.kanVeilede, builder.kanOverstyre,
            builder.kanOppgavestyre, builder.kanBehandleKode6, LocalDateTime.now());
    }

    public static InnloggetNavAnsattDto ukjentNavAnsatt(String brukernavn, String navn) {
        return new InnloggetNavAnsattDto(brukernavn, navn, false, false, false,
            false, false, LocalDateTime.now());
    }


    @Override
    public String toString() {
        return "InnloggetNavAnsattDto{" +
            "kanSaksbehandle=" + kanSaksbehandle +
            ", kanVeilede=" + kanVeilede +
            ", kanOverstyre=" + kanOverstyre +
            ", kanOppgavestyre=" + kanOppgavestyre +
            ", funksjonellTid=" + funksjonellTid + '}';
    }

    public static class Builder {
        private final String brukernavn;
        private final String navn;
        private boolean kanSaksbehandle;
        private boolean kanVeilede;
        private boolean kanOverstyre;
        private boolean kanOppgavestyre;
        private boolean kanBehandleKode6;

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

        public Builder kanOverstyre(boolean kanOverstyre) {
            this.kanOverstyre = kanOverstyre;
            return this;
        }

        public Builder kanOppgavestyre(boolean kanOppgavestyre) {
            this.kanOppgavestyre = kanOppgavestyre;
            return this;
        }

        public Builder kanBehandleKode6(boolean kanBehandleKode6) {
            this.kanBehandleKode6 = kanBehandleKode6;
            return this;
        }

        public InnloggetNavAnsattDto build() {
            return new InnloggetNavAnsattDto(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        InnloggetNavAnsattDto that = (InnloggetNavAnsattDto) o;
        return kanVeilede == that.kanVeilede && kanOverstyre == that.kanOverstyre
            && kanSaksbehandle == that.kanSaksbehandle && kanOppgavestyre == that.kanOppgavestyre && kanBehandleKode6 == that.kanBehandleKode6
            && Objects.equals(navn, that.navn) && Objects.equals(brukernavn, that.brukernavn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(brukernavn, navn, kanSaksbehandle, kanVeilede, kanOverstyre, kanOppgavestyre, kanBehandleKode6);
    }
}
