package no.nav.foreldrepenger.web.app.tjenester.fagsak.app;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

// View = respons fra applikasjonslag (velges foreløpig fremfor å la applikasjonslag bruke DTO direkte)
public class FagsakSamlingForBruker {

    private PersoninfoBasis brukerInfo;
    private List<FagsakRad> fagsakInfoer = new ArrayList<>();

    private FagsakSamlingForBruker() {
    }

    FagsakSamlingForBruker(PersoninfoBasis brukerInfo) {
        this.brukerInfo = brukerInfo;
    }

    static FagsakSamlingForBruker emptyView() {
        return new FagsakSamlingForBruker();
    }

    public PersoninfoBasis getBrukerInfo() {
        return brukerInfo;
    }

    public List<FagsakRad> getFagsakInfoer() {
        return fagsakInfoer;
    }

    public boolean isEmpty() {
        return brukerInfo == null;
    }

    void leggTil(Fagsak fagsak, Integer antallBarn, LocalDate fødselsdato, Dekningsgrad dekningsgrad) {
        fagsakInfoer.add(new FagsakRad(fagsak, antallBarn, fødselsdato, dekningsgrad));
    }

    public static class FagsakRad {
        private final Fagsak fagsak;
        private Integer antallBarn;
        private final LocalDate fødselsdato;
        private Dekningsgrad dekningsgrad;

        private FagsakRad(Fagsak fagsak, Integer antallBarn, LocalDate fødselsdato, Dekningsgrad dekningsgrad) {
            this.fagsak = fagsak;
            this.antallBarn = antallBarn;
            this.fødselsdato = fødselsdato;
            this.dekningsgrad = dekningsgrad;
        }

        public Fagsak getFagsak() {
            return fagsak;
        }

        public LocalDate getFødselsdato() {
            return fødselsdato;
        }

        public Integer getAntallBarn() {
            return antallBarn;
        }

        public Optional<Dekningsgrad> getDekningsgrad() {
            return Optional.ofNullable(dekningsgrad);
        }
    }
}
