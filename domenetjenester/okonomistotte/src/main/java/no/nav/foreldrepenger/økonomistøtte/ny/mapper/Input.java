package no.nav.foreldrepenger.økonomistøtte.ny.mapper;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.OverordnetOppdragKjedeOversikt;

public class Input {
    private GruppertYtelse tilkjentYtelse;
    private OverordnetOppdragKjedeOversikt tidligereOppdrag;
    private String brukerFnr;
    private Long behandlingId;
    private Saksnummer saksnummer;
    private FagsakYtelseType fagsakYtelseType;
    private FamilieYtelseType familieYtelseType;
    private String ansvarligSaksbehandler;
    private LocalDate vedtaksdato;
    private boolean brukInntrekk;
    private Long prosessTaskId;

    private Input() {
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

    public FamilieYtelseType getFamilieYtelseType() {
        return familieYtelseType;
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
        private Input kladd = new Input();

        public Input build() {
            Objects.requireNonNull(kladd.tilkjentYtelse);
            Objects.requireNonNull(kladd.saksnummer);
            Objects.requireNonNull(kladd.behandlingId);
            Objects.requireNonNull(kladd.fagsakYtelseType);
            Objects.requireNonNull(kladd.vedtaksdato);
            Objects.requireNonNull(kladd.brukerFnr);
            if (kladd.fagsakYtelseType == FagsakYtelseType.FORELDREPENGER) {
                if (kladd.familieYtelseType != FamilieYtelseType.FØDSEL && kladd.familieYtelseType != FamilieYtelseType.ADOPSJON) {
                    throw ugyldigKombinasjon(kladd.fagsakYtelseType, kladd.familieYtelseType);
                }
            } else if (kladd.fagsakYtelseType == FagsakYtelseType.SVANGERSKAPSPENGER) {
                if (kladd.familieYtelseType != FamilieYtelseType.SVANGERSKAPSPENGER) {
                    throw ugyldigKombinasjon(kladd.fagsakYtelseType, kladd.familieYtelseType);
                }
            } else if (kladd.fagsakYtelseType == FagsakYtelseType.ENGANGSTØNAD) {
                if (kladd.familieYtelseType != null) {
                    throw ugyldigKombinasjon(kladd.fagsakYtelseType, kladd.familieYtelseType);
                }
            }
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

        public Builder medFamilieYtelseType(FamilieYtelseType familieYtelseType) {
            kladd.familieYtelseType = familieYtelseType;
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

        private static IllegalArgumentException ugyldigKombinasjon(FagsakYtelseType fagsakYtelseType, FamilieYtelseType familieYtelseType) {
            return new IllegalArgumentException("Ugyldig kombinasjon ytelseType " + fagsakYtelseType + " og familieYtelseType " + familieYtelseType);
        }
    }
}
