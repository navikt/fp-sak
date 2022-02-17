package no.nav.foreldrepenger.web.app.tjenester.fagsak.app;

import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.get;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.ProsesseringAsynkTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.VurderProsessTaskStatusForPollingApi;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.AktoerInfoDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.PersonDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SakHendelseDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SakPersonerDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.util.StringUtils;

@ApplicationScoped
public class FagsakTjeneste {

    private FagsakRepository fagsakRepository;

    private PersoninfoAdapter personinfoAdapter;
    private BehandlingRepository behandlingRepository;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;

    protected FagsakTjeneste() {
        // CDI runner
    }

    @Inject
    public FagsakTjeneste(FagsakRepository fagsakRepository,
                          BehandlingRepository behandlingRepository,
                          ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
                          PersoninfoAdapter personinfoAdapter,
                          FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                          FamilieHendelseTjeneste familieHendelseTjeneste,
                          PersonopplysningTjeneste personopplysningTjeneste,
                          DekningsgradTjeneste dekningsgradTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
    }

    public Optional<AsyncPollingStatus> sjekkProsessTaskPågår(Saksnummer saksnummer, String gruppe) {

        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
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

    public List<FagsakDto> søkFagsakDto(String søkestreng) {
        if (PersonIdent.erGyldigFnr(søkestreng)) {
            return hentFagsakDtoForFnr(new PersonIdent(søkestreng));
        }
        if (AktørId.erGyldigAktørId(søkestreng)) {
            return hentFagsakDtoForAktørId(new AktørId(søkestreng));
        }

        try {
            return lagFagsakDtoForSaksnummer(new Saksnummer(søkestreng)).stream().collect(Collectors.toList());
        } catch (Exception e) { // Ugyldig saksnummer
            return List.of();
        }
    }

    private List<FagsakDto> hentFagsakDtoForFnr(PersonIdent fnr) {
        return personinfoAdapter.hentAktørForFnr(fnr).map(this::hentFagsakDtoForAktørId).orElse(List.of());
    }

    private List<FagsakDto> hentFagsakDtoForAktørId(AktørId aktørId) {
        return fagsakRepository.hentForBruker(aktørId).stream().map(this::mapFraFagsakTilFagsakDto).collect(Collectors.toList());
    }

    public Optional<FagsakDto> hentFagsakDtoForSaksnummer(Saksnummer saksnummer) {
        return lagFagsakDtoForSaksnummer(saksnummer);
    }

    public Optional<SakPersonerDto> lagSakPersonerDto(Saksnummer saksnummer) {
        var fagsak = hentFagsakForSaksnummer(saksnummer).orElse(null);
        var brukerinfo = hentBruker(saksnummer).orElse(null);
        if (fagsak == null || brukerinfo == null) {
            return Optional.empty();
        }
        var språk = Optional.ofNullable(fagsak.getNavBruker()).map(NavBruker::getSpråkkode).orElse(Språkkode.NB);
        var bruker = mapFraPersoninfoBasisTilPersonDto(brukerinfo, språk);
        var annenPart = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)
            .flatMap(fr -> fr.getRelatertFagsak(fagsak))
            .map(Fagsak::getAktørId)
            .or(() -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
                .flatMap(b -> personopplysningTjeneste.hentOppgittAnnenPartAktørId(b.getId())))
            .flatMap(personinfoAdapter::hentBrukerBasisForAktør)
            .map(FagsakTjeneste::mapFraPersoninfoBasisTilPersonDto)
            .orElseGet(() -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
                .flatMap(b -> personopplysningTjeneste.hentOppgittAnnenPart(b.getId()))
                .filter(ap -> ap.getAktørId() == null && ap.getUtenlandskFnrLand() != null && !Landkoder.UDEFINERT.equals(ap.getUtenlandskFnrLand()))
                .map(ap -> new PersonDto(null, null, null, null, null, null, null, null))
                .orElse(null));
        var fh = hentFamilieHendelse(fagsak);
        return Optional.of(new SakPersonerDto(bruker, annenPart, fh.orElse(null)));
    }

    public Optional<AktoerInfoDto> lagAktoerInfoDto(AktørId aktørId) {
        var personinfo = personinfoAdapter.hentBrukerBasisForAktør(aktørId).orElse(null);
        if (personinfo == null) {
            return Optional.empty();
        }
        var personDto = mapFraPersoninfoBasisTilPersonDto(personinfo);
        var fagsakDtoer = fagsakRepository.hentForBruker(aktørId)
            .stream()
            .map(fagsak -> mapFraFagsakTilFagsakDto(fagsak))
            .collect(Collectors.toList());
        var aktoerInfoDto = new AktoerInfoDto(personinfo.aktørId().getId(), personDto, fagsakDtoer);
        return Optional.of(aktoerInfoDto);
    }

    private Optional<PersoninfoBasis> hentBruker(Saksnummer saksnummer) {
        return hentFagsakForSaksnummer(saksnummer).map(Fagsak::getAktørId).flatMap(personinfoAdapter::hentBrukerBasisForAktør);
    }

    private Integer finnDekningsgrad(Saksnummer saksnummer) {
        return dekningsgradTjeneste.finnDekningsgrad(saksnummer).map(Dekningsgrad::getVerdi).orElse(null);
    }

    private static PersonDto mapFraPersoninfoBasisTilPersonDto(PersoninfoBasis pi) {
        return mapFraPersoninfoBasisTilPersonDto(pi, Språkkode.NB);
    }

    private static PersonDto mapFraPersoninfoBasisTilPersonDto(PersoninfoBasis pi, Språkkode språkkode) {
        return new PersonDto(pi.aktørId().getId(), StringUtils.formaterMedStoreOgSmåBokstaver(pi.navn()), pi.personIdent().getIdent(), pi.kjønn(),
            pi.diskresjonskode(), pi.fødselsdato(), pi.dødsdato(), språkkode);
    }

    private Optional<FagsakDto> lagFagsakDtoForSaksnummer(Saksnummer saksnummer) {
        return fagsakRepository.hentSakGittSaksnummer(saksnummer).map(this::mapFraFagsakTilFagsakDto);
    }

    private FagsakDto mapFraFagsakTilFagsakDto(Fagsak fagsak) {
        var fh = hentFamilieHendelse(fagsak);
        return new FagsakDto(fagsak, fh.map(SakHendelseDto::getHendelseDato).orElse(null), fagsak.getRelasjonsRolleType(),
            finnDekningsgrad(fagsak.getSaksnummer()), lagLenker(fagsak), lagLenkerEngangshent(fagsak));
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

    private static List<ResourceLink> lagLenker(Fagsak fagsak) {
        List<ResourceLink> lenkene = new ArrayList<>();
        var saksnummer = new SaksnummerDto(fagsak.getSaksnummer());
        lenkene.add(get(FagsakRestTjeneste.RETTIGHETER_PATH, "sak-rettigheter", saksnummer));
        lenkene.add(get(HistorikkRestTjeneste.HISTORIKK_PATH, "sak-historikk", saksnummer));
        lenkene.add(get(DokumentRestTjeneste.DOKUMENTER_PATH, "sak-dokumentliste", saksnummer));
        lenkene.add(get(BehandlingRestTjeneste.BEHANDLINGER_ALLE_PATH, "sak-alle-behandlinger", saksnummer));
        lenkene.add(get(BehandlingRestTjeneste.ANNEN_PART_BEHANDLING_PATH, "sak-annen-part-behandling", saksnummer));
        return lenkene;
    }

    private static List<ResourceLink> lagLenkerEngangshent(Fagsak fagsak) {
        List<ResourceLink> lenkene = new ArrayList<>();
        var saksnummer = new SaksnummerDto(fagsak.getSaksnummer());
        lenkene.add(get(FagsakRestTjeneste.PERSONER_PATH, "sak-personer", saksnummer));
        return lenkene;
    }
}
