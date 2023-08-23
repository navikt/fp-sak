package no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.OverordnetOppdragKjedeOversikt;

import java.time.LocalDate;
import java.util.Objects;

public class OppdragInput {
    private GruppertYtelse tilkjentYtelse;
    private OverordnetOppdragKjedeOversikt tidligereOppdrag;
    private String brukerFnr;
    private Long behandlingId;
    private Saksnummer saksnummer;
    private FagsakYtelseType fagsakYtelseType;
    private String ansvarligSaksbehandler;
    private LocalDate vedtaksdato;
    private boolean brukInntrekk;
    private Long prosessTaskId;

    private OppdragInput() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public GruppertYtelse getTilkjentYtelse() {
        return tilkjentYtelse;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public FagsakYtelseType getYtelseType() {
        return fagsakYtelseType;
    }

    public String getAnsvarligSaksbehandler() {
        return ansvarligSaksbehandler;
    }

    public LocalDate getVedtaksdato() {
        return vedtaksdato;
    }

    public boolean brukInntrekk() {
        return brukInntrekk;
    }

    public Long getProsessTaskId() {
        return prosessTaskId;
    }

    public OverordnetOppdragKjedeOversikt getTidligereOppdrag() {
        return tidligereOppdrag;
    }

    public String getBrukerFnr() {
        return brukerFnr;
    }

    public static class Builder {
        private OppdragInput kladd = new OppdragInput();

        public OppdragInput build() {
            Objects.requireNonNull(kladd.tilkjentYtelse);
            Objects.requireNonNull(kladd.saksnummer);
            Objects.requireNonNull(kladd.behandlingId);
            Objects.requireNonNull(kladd.fagsakYtelseType);
            Objects.requireNonNull(kladd.vedtaksdato);
            Objects.requireNonNull(kladd.brukerFnr);

            try {
                return kladd;
            } finally {
                kladd = null;
            }
        }

        public Builder medTilkjentYtelse(GruppertYtelse tilkjentYtelse) {
            kladd.tilkjentYtelse = tilkjentYtelse;
            return this;
        }

        public Builder medBehandlingId(Long behandlingId) {
            kladd.behandlingId = behandlingId;
            return this;
        }

        public Builder medSaksnummer(Saksnummer saksnummer) {
            kladd.saksnummer = saksnummer;
            return this;
        }

        public Builder medFagsakYtelseType(FagsakYtelseType fagsakYtelseType) {
            kladd.fagsakYtelseType = fagsakYtelseType;
            return this;
        }

        public Builder medAnsvarligSaksbehandler(String ansvarligSaksbehandler) {
            kladd.ansvarligSaksbehandler = ansvarligSaksbehandler;
            return this;
        }

        public Builder medVedtaksdato(LocalDate vedtaksdato) {
            kladd.vedtaksdato = vedtaksdato;
            return this;
        }

        public Builder medBrukInntrekk(boolean brukInntrekk) {
            kladd.brukInntrekk = brukInntrekk;
            return this;
        }

        public Builder medProsessTaskId(Long prosessTaskId) {
            kladd.prosessTaskId = prosessTaskId;
            return this;
        }

        public Builder medBrukerFnr(String brukerFnr) {
            kladd.brukerFnr = brukerFnr;
            return this;
        }

        public Builder medTidligereOppdrag(OverordnetOppdragKjedeOversikt tidligereOppdrag) {
            kladd.tidligereOppdrag = tidligereOppdrag;
            return this;
        }
    }
}
