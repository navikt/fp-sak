package no.nav.foreldrepenger.web.app.tjenester.fagsak.app;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.VurderProsessTaskStatusForPollingApi;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
public class FagsakApplikasjonTjeneste {
    private static FagsakProsessTaskFeil FEIL = FeilFactory.create(FagsakProsessTaskFeil.class);

    private FagsakRepository fagsakRespository;

    private PersoninfoAdapter personinfoAdapter;
    private BehandlingRepository behandlingRepository;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;

    private Predicate<String> predikatErFnr = søkestreng -> søkestreng.matches("\\d{11}");
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;

    protected FagsakApplikasjonTjeneste() {
        // CDI runner
    }

    @Inject
    public FagsakApplikasjonTjeneste(FagsakRepository fagsakRespository,
            BehandlingRepository behandlingRepository,
            ProsesseringAsynkTjeneste prosesseringAsynkTjeneste, PersoninfoAdapter personinfoAdapter,
            FamilieHendelseTjeneste familieHendelseTjeneste,
            DekningsgradTjeneste dekningsgradTjeneste) {

        this.fagsakRespository = fagsakRespository;
        this.personinfoAdapter = personinfoAdapter;
        this.behandlingRepository = behandlingRepository;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
    }

    public Optional<AsyncPollingStatus> sjekkProsessTaskPågår(Saksnummer saksnummer, String gruppe) {

        Optional<Fagsak> fagsak = fagsakRespository.hentSakGittSaksnummer(saksnummer);
        if (fagsak.isPresent()) {
            Long fagsakId = fagsak.get().getId();
            Map<String, ProsessTaskData> nesteTask = prosesseringAsynkTjeneste.sjekkProsessTaskPågår(fagsakId, null, gruppe);
            return new VurderProsessTaskStatusForPollingApi(FEIL, fagsakId).sjekkStatusNesteProsessTask(gruppe, nesteTask);
        } else {
            return Optional.empty();
        }

    }

    public FagsakSamlingForBruker hentSaker(String søkestreng) {
        if (!søkestreng.matches("\\d+")) {
            return FagsakSamlingForBruker.emptyView();
        }

        if (predikatErFnr.test(søkestreng)) {
            return hentSakerForFnr(new PersonIdent(søkestreng));
        } else {
            return hentFagsakForSaksnummer(new Saksnummer(søkestreng));
        }
    }

    private FagsakSamlingForBruker hentSakerForFnr(PersonIdent fnr) {
        Optional<Personinfo> funnetNavBruker = personinfoAdapter.hentBrukerForFnr(fnr);
        if (funnetNavBruker.isEmpty()) {
            return FagsakSamlingForBruker.emptyView();
        }
        List<Fagsak> fagsaker = fagsakRespository.hentForBruker(funnetNavBruker.get().getAktørId());
        return tilFagsakView(fagsaker, finnAntallBarnTps(fagsaker), funnetNavBruker.get());
    }

    /** Returnerer samling med kun en fagsak. */
    public FagsakSamlingForBruker hentFagsakForSaksnummer(Saksnummer saksnummer) {
        Optional<Fagsak> fagsak = fagsakRespository.hentSakGittSaksnummer(saksnummer);
        if (fagsak.isEmpty()) {
            return FagsakSamlingForBruker.emptyView();
        }
        List<Fagsak> fagsaker = Collections.singletonList(fagsak.get());
        AktørId aktørId = fagsak.get().getNavBruker().getAktørId();

        Optional<Personinfo> funnetNavBruker = personinfoAdapter.hentBrukerForAktør(aktørId);
        if (funnetNavBruker.isEmpty()) {
            return FagsakSamlingForBruker.emptyView();
        }

        return tilFagsakView(fagsaker, finnAntallBarnTps(fagsaker), funnetNavBruker.get());
    }

    private FagsakSamlingForBruker tilFagsakView(List<Fagsak> fagsaker, Map<Long, Integer> antallBarnPerFagsak, Personinfo personinfo) {
        FagsakSamlingForBruker view = new FagsakSamlingForBruker(personinfo);
        fagsaker.forEach(sak -> {
            var dekningsgrad = dekningsgradTjeneste.finnDekningsgrad(sak.getSaksnummer());
            view.leggTil(sak, antallBarnPerFagsak.get(sak.getId()), hentBarnsFødselsdato(sak), dekningsgrad.orElse(null));
        });
        return view;
    }

    private LocalDate hentBarnsFødselsdato(Fagsak fagsak) {
        final Optional<Behandling> behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandling.isPresent()) {
            final Optional<FamilieHendelseEntitet> bekreftetFødsel = familieHendelseTjeneste.finnAggregat(behandling.get().getId())
                    .flatMap(FamilieHendelseGrunnlagEntitet::getGjeldendeBekreftetVersjon)
                    .filter(hendelse -> hendelse.getType().equals(FamilieHendelseType.FØDSEL));
            if (bekreftetFødsel.isPresent()) {
                return bekreftetFødsel.get().getBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst().orElse(null);
            }
        }
        return null;
    }

    private Map<Long, Integer> finnAntallBarnTps(List<Fagsak> fagsaker) {
        Map<Long, Integer> antallBarnPerFagsak = new HashMap<>();
        for (Fagsak fagsak : fagsaker) {
            antallBarnPerFagsak.put(fagsak.getId(), 0); // FIXME: Skal ikke være hardkodet.
        }
        return antallBarnPerFagsak;
    }

}
