package no.nav.foreldrepenger.web.app.tjenester.fagsak.app;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingOpprettingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.AnnenPartBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.FagsakBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.FagsakBehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkRequestPath;
import no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkV2Tjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakFullDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakNotatDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.PersonDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SakHendelseDto;
import no.nav.foreldrepenger.web.app.util.StringUtils;

@ApplicationScoped
public class FagsakFullTjeneste {
    private FagsakRepository fagsakRepository;

    private PersoninfoAdapter personinfoAdapter;
    private BehandlingRepository behandlingRepository;

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FagsakEgenskapRepository fagsakEgenskapRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;

    private BehandlingsoppretterTjeneste behandlingsoppretterTjeneste;
    private FagsakBehandlingDtoTjeneste behandlingDtoTjeneste;

    private HistorikkV2Tjeneste historikkV2Tjeneste;

    protected FagsakFullTjeneste() {
        // CDI runner
    }

    @Inject
    public FagsakFullTjeneste(FagsakRepository fagsakRepository,  // NOSONAR
                              BehandlingRepository behandlingRepository,
                              PersoninfoAdapter personinfoAdapter,
                              FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                              YtelseFordelingTjeneste ytelseFordelingTjeneste,
                              FagsakEgenskapRepository fagsakEgenskapRepository,
                              FamilieHendelseTjeneste familieHendelseTjeneste,
                              PersonopplysningTjeneste personopplysningTjeneste,
                              BehandlingsoppretterTjeneste behandlingsoppretterTjeneste,
                              FagsakBehandlingDtoTjeneste behandlingDtoTjeneste,
                              HistorikkV2Tjeneste historikkV2Tjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingsoppretterTjeneste = behandlingsoppretterTjeneste;
        this.behandlingDtoTjeneste = behandlingDtoTjeneste;
        this.historikkV2Tjeneste = historikkV2Tjeneste;
    }

    public Optional<FagsakFullDto> hentFullFagsakDtoForSaksnummer(HttpServletRequest request, Saksnummer saksnummer) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null) {
            return Optional.empty();
        }
        var familiehendelse = hentFamilieHendelse(fagsak).orElse(null);
        var dekningsgrad = finnDekningsgrad(fagsak);
        var annenpartSak = hentAnnenPartsGjeldendeYtelsesBehandling(fagsak).orElse(null);
        var bruker = lagBrukerPersonDto(fagsak).orElse(null);
        var manglerAdresse = personinfoAdapter.sjekkOmBrukerManglerAdresse(fagsak.getYtelseType(), fagsak.getAktørId());
        var annenpart = lagAnnenpartPersonDto(fagsak).orElse(null);
        var oppretting = Stream.of(BehandlingType.getYtelseBehandlingTyper(), BehandlingType.getAndreBehandlingTyper()).flatMap(Collection::stream)
            .map(bt -> new BehandlingOpprettingDto(bt, behandlingsoppretterTjeneste.kanOppretteNyBehandlingAvType(fagsak.getId(), bt)))
            .toList();
        var fagsakMarkeringer = fagsakEgenskapRepository.finnFagsakMarkeringer(fagsak.getId());
        var behandlinger = behandlingDtoTjeneste.lagBehandlingDtoer(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId()));
        var dokumentPath = HistorikkRequestPath.getRequestPath(request);
        var historikk = historikkV2Tjeneste.hentForSak(saksnummer, dokumentPath);
        var notater = fagsakRepository.hentFagsakNotater(fagsak.getId()).stream().map(FagsakNotatDto::fraNotat).toList();
        var ferskesteKontrollresultatBehandling = behandlingRepository.finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeFor(fagsak.getId(),
                BehandlingType.FØRSTEGANGSSØKNAD)
            .flatMap(førsteBeh -> behandlinger.stream().filter(beh -> beh.getUuid().equals(førsteBeh.getUuid())).findFirst())
            .map(FagsakBehandlingDto::getKontrollResultat);
        var dto = new FagsakFullDto(fagsak, dekningsgrad, bruker, manglerAdresse, annenpart, annenpartSak, familiehendelse, fagsakMarkeringer,
            oppretting, behandlinger, historikk, notater, ferskesteKontrollresultatBehandling.orElse(null));
        return Optional.of(dto);
    }

    private Optional<PersonDto> lagBrukerPersonDto(Fagsak fagsak) {
        return personinfoAdapter.hentBrukerBasisForAktør(fagsak.getYtelseType(), fagsak.getAktørId())
            .map(pi -> mapFraPersoninfoBasisTilPersonDto(pi, Optional.ofNullable(fagsak.getNavBruker()).map(NavBruker::getSpråkkode).orElse(Språkkode.NB)));
    }

    private Optional<PersonDto> lagAnnenpartPersonDto(Fagsak fagsak) {
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)
            .flatMap(fr -> fr.getRelatertFagsak(fagsak))
            .map(Fagsak::getAktørId)
            .or(() -> finnYtelsesBehandling(fagsak.getId())
                .flatMap(b -> personopplysningTjeneste.hentOppgittAnnenPartAktørId(b.getId())))
            .flatMap(a -> personinfoAdapter.hentBrukerBasisForAktør(fagsak.getYtelseType(), a))
            .map(FagsakFullTjeneste::mapFraPersoninfoBasisTilPersonDto)
            .or(() -> finnYtelsesBehandling(fagsak.getId())
                .flatMap(b -> personopplysningTjeneste.hentOppgittAnnenPart(b.getId()))
                .filter(ap -> ap.getAktørId() == null && ap.getUtenlandskFnrLand() != null && !Landkoder.UDEFINERT.equals(ap.getUtenlandskFnrLand()))
                .map(ap -> new PersonDto(null, null, null, null, null, null, null, null, null)));
    }

    private Integer finnDekningsgrad(Fagsak fagsak) {
        return finnYtelsesBehandling(fagsak.getId())
            .flatMap(b -> ytelseFordelingTjeneste.hentAggregatHvisEksisterer(b.getId()))
            .flatMap(yfa -> Optional.ofNullable(yfa.getGjeldendeDekningsgrad()))
            .map(Dekningsgrad::getVerdi)
            .orElse(null);
    }

    private static PersonDto mapFraPersoninfoBasisTilPersonDto(PersoninfoBasis pi) {
        return mapFraPersoninfoBasisTilPersonDto(pi, Språkkode.NB);
    }

    private static PersonDto mapFraPersoninfoBasisTilPersonDto(PersoninfoBasis pi, Språkkode språkkode) {
        return new PersonDto(pi.aktørId().getId(), StringUtils.formaterMedStoreOgSmåBokstaver(pi.navn()), pi.personIdent().getIdent(), pi.kjønn(),
            pi.diskresjonskode(), pi.fødselsdato(), pi.dødsdato(), pi.dødsdato(), språkkode);
    }

    private Optional<SakHendelseDto> hentFamilieHendelse(Fagsak fagsak) {
        return finnYtelsesBehandling(fagsak.getId())
            .flatMap(b -> familieHendelseTjeneste.finnAggregat(b.getId()))
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(h -> new SakHendelseDto(h.getType(), hendelseDato(h), h.getAntallBarn(),
                !h.getBarna().isEmpty() && h.getBarna().stream().allMatch(b -> b.getDødsdato().isPresent())));
    }

    private LocalDate hendelseDato(FamilieHendelseEntitet fh) {
        return Optional.ofNullable(fh.getSkjæringstidspunkt())
            .orElseGet(() -> fh.getTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null));
    }

    private Optional<AnnenPartBehandlingDto> hentAnnenPartsGjeldendeYtelsesBehandling(Fagsak fagsak) {
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)
            .flatMap(r -> r.getRelatertFagsakFraId(fagsak.getId()))
            .map(Fagsak::getId).flatMap(this::finnYtelsesBehandling)
            .map(behandling -> new AnnenPartBehandlingDto(behandling.getSaksnummer().getVerdi(),
                behandling.getFagsak().getRelasjonsRolleType(), behandling.getUuid()));
    }

    private Optional<Behandling> finnYtelsesBehandling(Long fagsakId) {
        return behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingReadOnlyFor(fagsakId)
            .or(() -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsakId));
    }
}
