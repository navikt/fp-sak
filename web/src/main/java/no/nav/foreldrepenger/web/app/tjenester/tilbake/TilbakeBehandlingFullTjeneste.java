package no.nav.foreldrepenger.web.app.tjenester.tilbake;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.Aktør;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
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
public class TilbakeBehandlingFullTjeneste {


    private ØkonomioppdragRepository økonomioppdragRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private VergeRepository vergeRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    public TilbakeBehandlingFullTjeneste(ØkonomioppdragRepository økonomioppdragRepository,
                                         TilbakekrevingRepository tilbakekrevingRepository,
                                         VergeRepository vergeRepository,
                                         BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                         FamilieHendelseTjeneste familieHendelseTjeneste,
                                         BehandlingVedtakRepository behandlingVedtakRepository) {
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.vergeRepository = vergeRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.behandlingVedtakRepository = behandlingVedtakRepository;

    }

    public TilbakeBehandlingFullTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }


    public TilbakeFullDto lagFpsakBehandlingFullDtoTjeneste(Behandling behandling) {
        var behandlingDto = getBehandlingDto(behandling);
        var fagsak = new TilbakeFullDto.FagsakDto(behandling.getAktørId().getId(),
            behandling.getSaksnummer().getVerdi(), mapTilYtelseType(behandling.getFagsakYtelseType()));
        var familieHendelseDto = getFamilieHendelseDto(behandling);
        var tilbakeValg = tilbakekrevingRepository.hent(behandling.getId())
            .map(tv -> new TilbakeFullDto.FeilutbetalingDto(mapTilFeilutbetalingValgDto(tv.getVidereBehandling()), varseltekst(tv.getVarseltekst())))
            .orElse(null);
        var sendtOppdrag = økonomioppdragRepository.finnOppdragForBehandling(behandling.getId()).isPresent();
        var verge = vergeRepository.hentAggregat(behandling.getId())
            .flatMap(VergeAggregat::getVerge)
            .map(this::mapTilVergeDto).orElse(null);
        return new TilbakeFullDto(behandlingDto, fagsak, familieHendelseDto, tilbakeValg, sendtOppdrag, verge);
    }

    private TilbakeFullDto.FamilieHendelseDto getFamilieHendelseDto(Behandling behandling) {
        var familieHendelse = familieHendelseTjeneste.finnAggregat(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon);
        var antallBarn = familieHendelse.map(FamilieHendelseEntitet::getAntallBarn).orElse(0);
        var familieHendelseType = familieHendelse.filter(FamilieHendelseEntitet::getGjelderAdopsjon).isPresent() ?
            TilbakeFullDto.FamilieHendelseType.ADOPSJON : TilbakeFullDto.FamilieHendelseType.FØDSEL;
        return new TilbakeFullDto.FamilieHendelseDto(familieHendelseType, antallBarn);
    }

    private TilbakeFullDto.BehandlingDto getBehandlingDto(Behandling behandling) {
        var henvisning = new TilbakeFullDto.HenvisningDto(behandling.getId());
        var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(behandling.getFagsak());
        var språk = mapSpråk(behandling.getFagsak().getNavBruker().getSpråkkode());
        var vedtaksdato = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId())
            .map(BehandlingVedtak::getVedtakstidspunkt)
            .map(LocalDateTime::toLocalDate).orElse(null);
        return new TilbakeFullDto.BehandlingDto(behandling.getUuid(), henvisning,
            enhet.enhetId(), enhet.enhetNavn(), språk, vedtaksdato);
    }

    private static TilbakeFullDto.YtelseType mapTilYtelseType(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case FORELDREPENGER -> TilbakeFullDto.YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> TilbakeFullDto.YtelseType.SVANGERSKAPSPENGER;
            case ENGANGSTØNAD -> TilbakeFullDto.YtelseType.ENGANGSSTØNAD;
            case UDEFINERT -> throw new IllegalStateException("Utviklerfeil: Udefinert ytelse type");
            case null -> throw new IllegalStateException("Utviklerfeil: Mangler ytelse type");
        };
    }

    private static TilbakeFullDto.Språkkode mapSpråk(Språkkode språkkode) {
        return switch (språkkode) {
            case NB -> TilbakeFullDto.Språkkode.NB;
            case NN -> TilbakeFullDto.Språkkode.NN;
            case EN -> TilbakeFullDto.Språkkode.EN;
            case UDEFINERT -> TilbakeFullDto.Språkkode.NB;
            case null -> TilbakeFullDto.Språkkode.NB;
        };
    }

    private static TilbakeFullDto.FeilutbetalingValg mapTilFeilutbetalingValgDto(TilbakekrevingVidereBehandling valg) {
        return switch (valg) {
            case INNTREKK -> TilbakeFullDto.FeilutbetalingValg.INNTREKK;
            case IGNORER_TILBAKEKREVING -> TilbakeFullDto.FeilutbetalingValg.IGNORER;
            case OPPRETT_TILBAKEKREVING -> TilbakeFullDto.FeilutbetalingValg.OPPRETT;
            case TILBAKEKR_OPPDATER -> TilbakeFullDto.FeilutbetalingValg.OPPDATER;
            case UDEFINIERT ->  null;
            case null ->  null;
        };
    }

    private TilbakeFullDto.VergeDto mapTilVergeDto(VergeEntitet verge) {
        return new TilbakeFullDto.VergeDto(mapTilVergetype(verge.getVergeType()),
                verge.getBruker().map(Aktør::getAktørId).map(AktørId::getId).orElse(null),
                verge.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getNavn).orElse(null),
                verge.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getOrganisasjonsnummer).orElse(null),
                verge.getGyldigFom(), verge.getGyldigTom());

    }

    private static TilbakeFullDto.VergeType mapTilVergetype(VergeType vergeType) {
        return switch (vergeType) {
            case BARN -> TilbakeFullDto.VergeType.BARN;
            case FBARN -> TilbakeFullDto.VergeType.FORELDRELØS;
            case VOKSEN -> TilbakeFullDto.VergeType.VOKSEN;
            case ADVOKAT -> TilbakeFullDto.VergeType.ADVOKAT;
            case ANNEN_F -> TilbakeFullDto.VergeType.FULLMEKTIG;
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
