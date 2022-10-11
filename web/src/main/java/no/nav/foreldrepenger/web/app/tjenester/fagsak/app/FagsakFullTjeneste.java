package no.nav.foreldrepenger.web.app.tjenester.fagsak.app;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsoppretterTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingOpprettingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.AnnenPartBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakFullDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.PersonDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SakHendelseDto;
import no.nav.foreldrepenger.web.app.util.StringUtils;

@ApplicationScoped
public class FagsakFullTjeneste {

    private FagsakRepository fagsakRepository;

    private PersoninfoAdapter personinfoAdapter;
    private BehandlingRepository behandlingRepository;

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;

    private BehandlingsoppretterTjeneste behandlingsoppretterTjeneste;
    private BehandlingDtoTjeneste behandlingDtoTjeneste;

    protected FagsakFullTjeneste() {
        // CDI runner
    }

    @Inject
    public FagsakFullTjeneste(FagsakRepository fagsakRepository,
                              BehandlingRepository behandlingRepository,
                              PersoninfoAdapter personinfoAdapter,
                              FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                              FamilieHendelseTjeneste familieHendelseTjeneste,
                              PersonopplysningTjeneste personopplysningTjeneste,
                              BehandlingsoppretterTjeneste behandlingsoppretterTjeneste,
                              BehandlingDtoTjeneste behandlingDtoTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.behandlingRepository = behandlingRepository;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingsoppretterTjeneste = behandlingsoppretterTjeneste;
        this.behandlingDtoTjeneste = behandlingDtoTjeneste;
    }

    public Optional<FagsakFullDto> hentFullFagsakDtoForSaksnummer(Saksnummer saksnummer) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElse(null);
        if (fagsak == null) {
            return Optional.empty();
        }
        var familiehendelse = hentFamilieHendelse(fagsak).orElse(null);
        var dekningsgrad = finnDekningsgrad(fagsak);
        var annenpartSak = hentAnnenPartsGjeldendeYtelsesBehandling(fagsak).orElse(null);
        var bruker = lagBrukerPersonDto(fagsak).orElse(null);
        var manglerAdresse = personinfoAdapter.sjekkOmBrukerManglerAdresse(fagsak.getAktørId());
        var annenpart = lagAnnenpartPersonDto(fagsak).orElse(null);
        var oppretting = Stream.of(BehandlingType.getYtelseBehandlingTyper(), BehandlingType.getAndreBehandlingTyper()).flatMap(Collection::stream)
            .map(bt -> new BehandlingOpprettingDto(bt, behandlingsoppretterTjeneste.kanOppretteNyBehandlingAvType(fagsak.getId(), bt)))
            .toList();
        var behandlinger = behandlingDtoTjeneste.lagBehandlingDtoer(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsak.getId()));
        var dto = new FagsakFullDto(fagsak, dekningsgrad, bruker, manglerAdresse, annenpart, annenpartSak, familiehendelse, oppretting, behandlinger);
        return Optional.of(dto);
    }

    private Optional<PersonDto> lagBrukerPersonDto(Fagsak fagsak) {
        return personinfoAdapter.hentBrukerBasisForAktør(fagsak.getAktørId())
            .map(pi -> mapFraPersoninfoBasisTilPersonDto(pi, Optional.ofNullable(fagsak.getNavBruker()).map(NavBruker::getSpråkkode).orElse(Språkkode.NB)));
    }

    private Optional<PersonDto> lagAnnenpartPersonDto(Fagsak fagsak) {
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)
            .flatMap(fr -> fr.getRelatertFagsak(fagsak))
            .map(Fagsak::getAktørId)
            .or(() -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
                .flatMap(b -> personopplysningTjeneste.hentOppgittAnnenPartAktørId(b.getId())))
            .flatMap(personinfoAdapter::hentBrukerBasisForAktør)
            .map(FagsakFullTjeneste::mapFraPersoninfoBasisTilPersonDto)
            .or(() -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
                .flatMap(b -> personopplysningTjeneste.hentOppgittAnnenPart(b.getId()))
                .filter(ap -> ap.getAktørId() == null && ap.getUtenlandskFnrLand() != null && !Landkoder.UDEFINERT.equals(ap.getUtenlandskFnrLand()))
                .map(ap -> new PersonDto(null, null, null, null, null, null, null, null, null)));
    }

    private Integer finnDekningsgrad(Fagsak fagsak) {
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)
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

    private Optional<AnnenPartBehandlingDto> hentAnnenPartsGjeldendeYtelsesBehandling(Fagsak fagsak) {
        return fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(fagsak)
            .flatMap(r -> r.getRelatertFagsakFraId(fagsak.getId()))
            .map(Fagsak::getId).flatMap(behandlingRepository::hentSisteYtelsesBehandlingForFagsakId)
            .map(behandling -> new AnnenPartBehandlingDto(behandling.getFagsak().getSaksnummer().getVerdi(), behandling.getUuid()));
    }
}
