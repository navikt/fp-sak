package no.nav.foreldrepenger.web.app.tjenester.fagsak.app;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.VurderProsessTaskStatusForPollingApi;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.*;
import no.nav.foreldrepenger.web.app.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class FagsakTjeneste {

    private FagsakRepository fagsakRepository;

    private PersoninfoAdapter personinfoAdapter;
    private BehandlingRepository behandlingRepository;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    protected FagsakTjeneste() {
        // CDI runner
    }

    @Inject
    public FagsakTjeneste(FagsakRepository fagsakRepository,
                          BehandlingRepository behandlingRepository,
                          ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
                          PersoninfoAdapter personinfoAdapter,
                          FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                          FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
    }

    public Optional<AsyncPollingStatus> sjekkProsessTaskPågår(Saksnummer saksnummer, String gruppe) {

        var fagsak = hentFagsakForSaksnummer(saksnummer);
        if (fagsak.isPresent()) {
            var fagsakId = fagsak.get().getId();
            var nesteTask = prosesseringAsynkTjeneste.sjekkProsessTaskPågår(fagsakId, null, gruppe);
            return new VurderProsessTaskStatusForPollingApi(fagsakId).sjekkStatusNesteProsessTask(gruppe, nesteTask);
        }
        return Optional.empty();
    }

    public Optional<Fagsak> hentFagsakForSaksnummer(Saksnummer saksnummer) {
        return fagsakRepository.hentSakGittSaksnummer(saksnummer);
    }

    public List<FagsakSøkDto> søkFagsakDto(String søkestreng) {
        if (!søkestreng.matches("\\d+")) {
            return List.of();
        }
        if (PersonIdent.erGyldigFnr(søkestreng)) {
            return hentFagsakSøkDtoForFnr(new PersonIdent(søkestreng));
        }
        if (AktørId.erGyldigAktørId(søkestreng)) {
            return hentFagsakSøkDtoForAktørId(new AktørId(søkestreng));
        }

        try {
            var sak = hentFagsakForSaksnummer(new Saksnummer(søkestreng));
            var person = sak.map(Fagsak::getAktørId).flatMap(personinfoAdapter::hentBrukerBasisForAktør).orElse(null);
            return sak.map(f -> mapFraFagsakTilFagsakSøkDto(f, person)).stream().toList();
        } catch (Exception e) { // Ugyldig saksnummer
            return List.of();
        }
    }

    private List<FagsakSøkDto> hentFagsakSøkDtoForFnr(PersonIdent fnr) {
        return personinfoAdapter.hentAktørForFnr(fnr).map(this::hentFagsakSøkDtoForAktørId).orElse(List.of());
    }

    private List<FagsakSøkDto> hentFagsakSøkDtoForAktørId(AktørId aktørId) {
        var brukerinfo = personinfoAdapter.hentBrukerBasisForAktør(aktørId).orElse(null);
        return fagsakRepository.hentForBruker(aktørId).stream().map(f -> mapFraFagsakTilFagsakSøkDto(f, brukerinfo)).toList();
    }

    public Optional<FagsakBackendDto> hentFagsakDtoForSaksnummer(Saksnummer saksnummer) {
        return hentFagsakForSaksnummer(saksnummer).map(this::mapFraFagsakTilFagsakDto);
    }

    public Optional<AktoerInfoDto> lagAktoerInfoDto(AktørId aktørId) {
        var personinfo = personinfoAdapter.hentBrukerBasisForAktør(aktørId).orElse(null);
        if (personinfo == null) {
            return Optional.empty();
        }
        var personDto = mapFraPersoninfoBasisTilPersonDto(personinfo);
        var fagsakDtoer = fagsakRepository.hentForBruker(aktørId)
            .stream()
            .map(f -> mapFraFagsakTilFagsakSøkDto(f, null))
            .toList();
        var aktoerInfoDto = new AktoerInfoDto(personinfo.aktørId().getId(), personDto, fagsakDtoer);
        return Optional.of(aktoerInfoDto);
    }

    public List<Behandling> hentBehandlingerMedÅpentAksjonspunkt(Fagsak fagsak) {
        return behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).stream()
            .filter(b -> !b.getÅpneAksjonspunkter().isEmpty() && b.getÅpneAksjonspunkter().stream().noneMatch(Aksjonspunkt::erAutopunkt))
            .toList();
    }

    public void lagreFagsakNotat(Fagsak fagsak, String notat) {
        fagsakRepository.lagreFagsakNotat(fagsak.getId(), notat);
    }

    public List<Behandling> hentÅpneBehandlinger(Fagsak fagsak) {
        return behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId());
    }

    private Integer finnDekningsgrad(Saksnummer saksnummer) {
        return fagsakRelasjonTjeneste.finnRelasjonHvisEksisterer(saksnummer)
            .map(FagsakRelasjon::getGjeldendeDekningsgrad)
            .map(Dekningsgrad::getVerdi).orElse(null);
    }

    private static PersonDto mapFraPersoninfoBasisTilPersonDto(PersoninfoBasis pi) {
        return mapFraPersoninfoBasisTilPersonDto(pi, Språkkode.NB);
    }

    private static PersonDto mapFraPersoninfoBasisTilPersonDto(PersoninfoBasis pi, Språkkode språkkode) {
        return new PersonDto(pi.aktørId().getId(), StringUtils.formaterMedStoreOgSmåBokstaver(pi.navn()), pi.personIdent().getIdent(), pi.kjønn(),
            pi.diskresjonskode(), pi.fødselsdato(), pi.dødsdato(), pi.dødsdato(), språkkode);
    }

    private FagsakBackendDto mapFraFagsakTilFagsakDto(Fagsak fagsak) {
        return new FagsakBackendDto(fagsak, finnDekningsgrad(fagsak.getSaksnummer()));
    }

    private FagsakSøkDto mapFraFagsakTilFagsakSøkDto(Fagsak fagsak, PersoninfoBasis pi) {
        var fh = hentFamilieHendelse(fagsak);
        var person = Optional.ofNullable(pi).map(FagsakTjeneste::mapFraPersoninfoBasisTilPersonDto).orElse(null);
        return new FagsakSøkDto(fagsak, person, fh.map(SakHendelseDto::hendelseDato).orElse(null));
    }

    private Optional<SakHendelseDto> hentFamilieHendelse(Fagsak fagsak) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .flatMap(b -> familieHendelseTjeneste.finnAggregat(b.getId()))
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(h -> new SakHendelseDto(h.getType(), hendelseDato(h), h.getAntallBarn(),
                !h.getBarna().isEmpty() && h.getBarna().stream().allMatch(b -> b.getDødsdato().isPresent())));
    }

    private LocalDate hendelseDato(FamilieHendelseEntitet fh) {
        return Optional.ofNullable(fh.getSkjæringstidspunkt())
            .orElseGet(() -> fh.getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null));
    }

}
