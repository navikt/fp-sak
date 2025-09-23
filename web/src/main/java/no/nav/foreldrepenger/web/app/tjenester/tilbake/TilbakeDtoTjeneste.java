package no.nav.foreldrepenger.web.app.tjenester.tilbake;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.Aktør;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeOrganisasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;


@ApplicationScoped
public class TilbakeDtoTjeneste {


    private ØkonomioppdragRepository økonomioppdragRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private VergeRepository vergeRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private SøknadRepository søknadRepository;

    @Inject
    public TilbakeDtoTjeneste(ØkonomioppdragRepository økonomioppdragRepository,
                              TilbakekrevingRepository tilbakekrevingRepository,
                              VergeRepository vergeRepository,
                              BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                              FamilieHendelseTjeneste familieHendelseTjeneste,
                              BehandlingVedtakRepository behandlingVedtakRepository,
                              SøknadRepository søknadRepository) {
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.vergeRepository = vergeRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.søknadRepository = søknadRepository;
    }

    public TilbakeDtoTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }


    public TilbakeDto lagTilbakeDto(Behandling behandling) {
        var behandlingDto = getBehandlingDto(behandling);
        var fagsak = new TilbakeDto.FagsakDto(behandling.getAktørId().getId(),
            behandling.getSaksnummer().getVerdi(), mapTilYtelseType(behandling.getFagsakYtelseType()));
        var familieHendelseDto = getFamilieHendelseDto(behandling);
        var tilbakeValg = tilbakekrevingRepository.hent(behandling.getId())
            .map(tv -> new TilbakeDto.FeilutbetalingDto(mapTilFeilutbetalingValgDto(tv.getVidereBehandling()), varseltekst(tv.getVarseltekst())))
            .orElse(null);
        var sendtOppdrag = økonomioppdragRepository.finnOppdragForBehandling(behandling.getId()).isPresent();
        var verge = vergeRepository.hentAggregat(behandling.getId())
            .flatMap(VergeAggregat::getVerge)
            .map(this::mapTilVergeDto).orElse(null);
        return new TilbakeDto(behandlingDto, fagsak, familieHendelseDto, tilbakeValg, sendtOppdrag, verge);
    }

    private TilbakeDto.FamilieHendelseDto getFamilieHendelseDto(Behandling behandling) {
        var familieHendelse = familieHendelseTjeneste.finnAggregat(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon);
        var antallBarn = familieHendelse.map(FamilieHendelseEntitet::getAntallBarn).orElse(0);
        var familieHendelseType = familieHendelse.filter(FamilieHendelseEntitet::getGjelderAdopsjon).isPresent() ?
            TilbakeDto.FamilieHendelseType.ADOPSJON : TilbakeDto.FamilieHendelseType.FØDSEL;
        return new TilbakeDto.FamilieHendelseDto(familieHendelseType, antallBarn);
    }

    private TilbakeDto.BehandlingDto getBehandlingDto(Behandling behandling) {
        var henvisning = new TilbakeDto.HenvisningDto(behandling.getId());
        var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandling.getFagsak());
        var språk = mapSpråk(getSpråkkode(behandling));
        var vedtaksdato = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId())
            .map(BehandlingVedtak::getVedtakstidspunkt)
            .map(LocalDateTime::toLocalDate).orElse(null);
        return new TilbakeDto.BehandlingDto(behandling.getUuid(), henvisning,
            enhet.enhetId(), enhet.enhetNavn(), språk, vedtaksdato);
    }

    private Språkkode getSpråkkode(Behandling behandling) {
        return søknadRepository.hentSøknadHvisEksisterer(behandling.getId())
            .map(SøknadEntitet::getSpråkkode)
            .orElseGet(() -> behandling.getFagsak().getNavBruker().getSpråkkode());
    }

    private static TilbakeDto.YtelseType mapTilYtelseType(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> TilbakeDto.YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> TilbakeDto.YtelseType.SVANGERSKAPSPENGER;
            case ENGANGSTØNAD -> TilbakeDto.YtelseType.ENGANGSSTØNAD;
            case UDEFINERT -> throw new IllegalStateException("Utviklerfeil: Udefinert ytelse type");
            case null -> throw new IllegalStateException("Utviklerfeil: Mangler ytelse type");
        };
    }

    private static TilbakeDto.Språkkode mapSpråk(Språkkode språkkode) {
        return switch (språkkode) {
            case NB -> TilbakeDto.Språkkode.NB;
            case NN -> TilbakeDto.Språkkode.NN;
            case EN -> TilbakeDto.Språkkode.EN;
            case UDEFINERT -> TilbakeDto.Språkkode.NB;
            case null -> TilbakeDto.Språkkode.NB;
        };
    }

    private static TilbakeDto.FeilutbetalingValg mapTilFeilutbetalingValgDto(TilbakekrevingVidereBehandling valg) {
        return switch (valg) {
            case INNTREKK -> TilbakeDto.FeilutbetalingValg.INNTREKK;
            case IGNORER_TILBAKEKREVING -> TilbakeDto.FeilutbetalingValg.IGNORER;
            case OPPRETT_TILBAKEKREVING -> TilbakeDto.FeilutbetalingValg.OPPRETT;
            case TILBAKEKR_OPPDATER -> TilbakeDto.FeilutbetalingValg.OPPDATER;
            case UDEFINIERT ->  null;
            case null ->  null;
        };
    }

    private TilbakeDto.VergeDto mapTilVergeDto(VergeEntitet verge) {
        return new TilbakeDto.VergeDto(mapTilVergetype(verge.getVergeType()),
                verge.getBruker().map(Aktør::getAktørId).map(AktørId::getId).orElse(null),
                verge.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getNavn).orElse(null),
                verge.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getOrganisasjonsnummer).orElse(null),
                verge.getGyldigFom(), verge.getGyldigTom());

    }

    private static TilbakeDto.VergeType mapTilVergetype(VergeType vergeType) {
        return switch (vergeType) {
            case BARN -> TilbakeDto.VergeType.BARN;
            case FBARN -> TilbakeDto.VergeType.FORELDRELØS;
            case VOKSEN -> TilbakeDto.VergeType.VOKSEN;
            case ADVOKAT -> TilbakeDto.VergeType.ADVOKAT;
            case ANNEN_F -> TilbakeDto.VergeType.FULLMEKTIG;
            case null -> throw new IllegalStateException("Verge mangler type");
        };
    }

    private static String varseltekst(String varseltekst) {
        if (varseltekst == null || varseltekst.isBlank()) {
            return null;
        } else  {
            return varseltekst;
        }
    }
}
