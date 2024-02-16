package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.get;
import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.post;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ManglendeOpplysningerVurderingDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ManueltArbeidsforholdDto;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.TotrinnTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktDtoMapper;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BekreftedeAksjonspunkterDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.anke.AnkeRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding.ArbeidOgInntektsmeldingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold.InntektArbeidYtelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.BeregningsgrunnlagRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.FeriepengegrunnlagRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdVersjonDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.ByttBehandlendeEnhetDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.GjenopptaBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.HenleggBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.ReåpneBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.SettBehandlingPaVentDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn.InnsynRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.KlageRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageFormKravAksjonspunktMellomlagringDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageVurderingResultatAksjonspunktMellomlagringDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.opptjening.OpptjeningRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.svp.SvangerskapspengerRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.UttakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon.DokumentasjonVurderingBehovDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BehandlingMedUttaksperioderDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta.FaktaUttakPeriodeDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.verge.VergeRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.YtelsefordelingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.familiehendelse.FamiliehendelseRestTjeneste;

/**
 * Bygger et sammensatt resultat av BehandlingDto ved å samle data fra ulike tjenester, for å kunne levere dette ut på en REST tjeneste.
 */

//FIXME (TEAM SVP) denne burde splittes til å støtte en enkelt ytelse
@ApplicationScoped
public class BehandlingDtoTjeneste {

    private BeregningTjeneste beregningTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private SøknadRepository søknadRepository;
    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private TotrinnTjeneste totrinnTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    private DokumentasjonVurderingBehovDtoTjeneste dokumentasjonVurderingBehovDtoTjeneste;
    private FaktaUttakPeriodeDtoTjeneste faktaUttakPeriodeDtoTjeneste;


    @Inject
    public BehandlingDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                 BeregningTjeneste beregningTjeneste,
                                 TilbakekrevingRepository tilbakekrevingRepository,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                 BehandlingDokumentRepository behandlingDokumentRepository,
                                 ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste,
                                 TotrinnTjeneste totrinnTjeneste,
                                 DokumentasjonVurderingBehovDtoTjeneste dokumentasjonVurderingBehovDtoTjeneste,
                                 FaktaUttakPeriodeDtoTjeneste faktaUttakPeriodeDtoTjeneste,
                                 FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.beregningTjeneste = beregningTjeneste;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.svangerskapspengerUttakResultatRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.totrinnTjeneste = totrinnTjeneste;
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.dokumentasjonVurderingBehovDtoTjeneste = dokumentasjonVurderingBehovDtoTjeneste;
        this.faktaUttakPeriodeDtoTjeneste = faktaUttakPeriodeDtoTjeneste;
    }

    BehandlingDtoTjeneste() {
        // for CDI proxy
    }

    public Optional<Behandling> hentAnnenPartsGjeldendeYtelsesBehandling(Saksnummer saksnummer) {
        return fagsakRelasjonTjeneste.finnRelasjonHvisEksisterer(saksnummer)
            .flatMap(r -> saksnummer.equals(r.getFagsakNrEn().getSaksnummer()) ? r.getFagsakNrTo() : Optional.of(r.getFagsakNrEn()))
            .map(Fagsak::getId)
            .flatMap(behandlingRepository::hentSisteYtelsesBehandlingForFagsakId);
    }

    private BehandlingDto lagBehandlingDto(Behandling behandling,
                                           Optional<BehandlingsresultatDto> behandlingsresultatDto,
                                           boolean erBehandlingMedGjeldendeVedtak, LocalDate vedtaksdato) {
        var dto = new BehandlingDto();
        var uuidDto = new UuidDto(behandling.getUuid());
        BehandlingDtoUtil.setStandardfelterMedGjeldendeVedtak(behandling, getBehandlingsresultat(behandling.getId()), dto, erBehandlingMedGjeldendeVedtak, vedtaksdato);
        dto.setSpråkkode(getSpråkkode(behandling));
        dto.setBehandlingsresultat(behandlingsresultatDto.orElse(null));

        if (behandling.erYtelseBehandling()) {
            dto.leggTil(get(PersonRestTjeneste.PERSONOVERSIKT_PATH, "behandling-personoversikt", uuidDto));
            dto.leggTil(get(FamiliehendelseRestTjeneste.FAMILIEHENDELSE_V2_PATH, "familiehendelse-v2", uuidDto));
        }

        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            dto.leggTil(get(KlageRestTjeneste.KLAGE_V2_PATH, "klage-vurdering", uuidDto));
        }

        // Totrinnsbehandling
        if (!BehandlingType.INNSYN.equals(behandling.getType()) && BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus())) {
            dto.leggTil(post(AksjonspunktRestTjeneste.AKSJONSPUNKT_PATH, "bekreft-totrinnsaksjonspunkt", uuidDto));
        }

        // Brev
        dto.leggTil(post(BrevRestTjeneste.BREV_BESTILL_PATH, "brev-bestill"));
        dto.leggTil(post(BrevRestTjeneste.BREV_VIS_PATH, "brev-vis"));

        return dto;
    }

    private Språkkode getSpråkkode(Behandling behandling) {
        if (!behandling.erYtelseBehandling()) {
            return behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(behandling.getFagsakId())
                .flatMap(s -> søknadRepository.hentSøknadHvisEksisterer(s.getId()))
                .map(SøknadEntitet::getSpråkkode)
                .orElseGet(()-> behandling.getFagsak().getNavBruker().getSpråkkode());
        }
        return søknadRepository.hentSøknadHvisEksisterer(behandling.getId()).map(SøknadEntitet::getSpråkkode).orElseGet(() -> behandling.getFagsak().getNavBruker().getSpråkkode());
    }

    public List<BehandlingDto> lagBehandlingDtoer(List<Behandling> behandlinger) {
        if (behandlinger.isEmpty()) {
            return Collections.emptyList();
        }
        var gjeldendeVedtak = behandlingVedtakRepository.hentGjeldendeVedtak(behandlinger.get(0).getFagsak());
        var behandlingMedGjeldendeVedtak = gjeldendeVedtak.map(BehandlingVedtak::getBehandlingsresultat).map(Behandlingsresultat::getBehandlingId).map(behandlingRepository::hentBehandling);
        return behandlinger.stream().map(behandling -> {
            var erBehandlingMedGjeldendeVedtak = erBehandlingMedGjeldendeVedtak(behandling, behandlingMedGjeldendeVedtak);
            var behandlingsresultatDto = lagBehandlingsresultatDto(behandling);
            var vedtaksdato = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId())
                .map(BehandlingVedtak::getVedtaksdato).orElse(null);
            return lagBehandlingDto(behandling, behandlingsresultatDto, erBehandlingMedGjeldendeVedtak, vedtaksdato);
        }).toList();
    }

    private boolean erBehandlingMedGjeldendeVedtak(Behandling behandling, Optional<Behandling> behandlingMedGjeldendeVedtak) {
        return behandlingMedGjeldendeVedtak.filter(b -> b.getId().equals(behandling.getId())).isPresent();
    }

    public UtvidetBehandlingDto lagUtvidetBehandlingDto(Behandling behandling, AsyncPollingStatus asyncStatus) {
        var sisteAvsluttedeIkkeHenlagteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId());
        var dto = mapFra(behandling, erBehandlingMedGjeldendeVedtak(behandling, sisteAvsluttedeIkkeHenlagteBehandling));
        if (asyncStatus != null && !asyncStatus.isPending()) {
            dto.setAsyncStatus(asyncStatus);
        }
        return dto;
    }

    private void settStandardfelterUtvidet(Behandling behandling, UtvidetBehandlingDto dto, boolean erBehandlingMedGjeldendeVedtak) {
        var vedtaksDato = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId())
            .map(BehandlingVedtak::getVedtaksdato)
            .orElse(null);
        BehandlingDtoUtil.settStandardfelterUtvidet(behandling, getBehandlingsresultat(behandling.getId()), dto, erBehandlingMedGjeldendeVedtak, vedtaksDato);
        dto.setSpråkkode(getSpråkkode(behandling));
        var behandlingsresultatDto = lagBehandlingsresultatDto(behandling);
        dto.setBehandlingsresultat(behandlingsresultatDto.orElse(null));
    }

    private void leggTilLenkerForBehandlingsoperasjoner(Behandling behandling, BehandlingDto dto) {
        // Felles for alle behandlingstyper
        dto.leggTil(post(BehandlingRestTjeneste.BYTT_ENHET_PATH, "bytt-behandlende-enhet", new ByttBehandlendeEnhetDto()));
        dto.leggTil(post(BehandlingRestTjeneste.HENLEGG_PATH, "henlegg-behandling", new HenleggBehandlingDto()));
        dto.leggTil(post(BehandlingRestTjeneste.GJENOPPTA_PATH, "gjenoppta-behandling", new GjenopptaBehandlingDto()));
        dto.leggTil(post(BehandlingRestTjeneste.SETT_PA_VENT_PATH, "sett-behandling-pa-vent", new SettBehandlingPaVentDto()));
        dto.leggTil(post(BehandlingRestTjeneste.ENDRE_VENTEFRIST_PATH, "endre-pa-vent", new SettBehandlingPaVentDto()));
        dto.leggTil(post(AksjonspunktRestTjeneste.AKSJONSPUNKT_PATH, "lagre-aksjonspunkter", new BekreftedeAksjonspunkterDto()));
        dto.leggTil(post(VergeRestTjeneste.VERGE_OPPRETT_PATH, "opprett-verge", new BehandlingIdVersjonDto()));
        dto.leggTil(post(VergeRestTjeneste.VERGE_FJERN_PATH, "fjern-verge", new BehandlingIdVersjonDto()));

        if (behandling.erYtelseBehandling()) {
            dto.leggTil(post(BehandlingRestTjeneste.OPNE_FOR_ENDRINGER_PATH, "opne-for-endringer", new ReåpneBehandlingDto()));
            dto.leggTil(post(AksjonspunktRestTjeneste.AKSJONSPUNKT_OVERSTYR_PATH, "lagre-overstyr-aksjonspunkter",
                new BekreftedeAksjonspunkterDto()));
            if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
                dto.leggTil(post(UttakRestTjeneste.STONADSKONTOER_GITT_UTTAKSPERIODER_PATH, "lagre-stonadskontoer-gitt-uttaksperioder",
                    new BehandlingMedUttaksperioderDto()));
            }
        }
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            dto.leggTil(post(KlageRestTjeneste.MELLOMLAGRE_PATH, "mellomlagre-klage", new KlageVurderingResultatAksjonspunktMellomlagringDto()));
            dto.leggTil(post(KlageRestTjeneste.MELLOMLAGRE_FORMKRAV_KLAGE_PATH, "mellomlagre-formkrav-klage", new KlageFormKravAksjonspunktMellomlagringDto()));
        }
    }

    private UtvidetBehandlingDto mapFra(Behandling behandling, boolean erBehandlingMedGjeldendeVedtak) {
        var dto = new UtvidetBehandlingDto();
        settStandardfelterUtvidet(behandling, dto, erBehandlingMedGjeldendeVedtak);

        var saksnummerDto = new SaksnummerDto(behandling.getFagsak().getSaksnummer());
        dto.leggTil(get(FagsakRestTjeneste.FAGSAK_PATH, "fagsak", saksnummerDto));

        leggTilLenkerForBehandlingsoperasjoner(behandling, dto);

        var uuidDto = new UuidDto(behandling.getUuid());
        dto.leggTil(get(AksjonspunktRestTjeneste.AKSJONSPUNKT_V2_PATH, "aksjonspunkter", uuidDto));
        var aksjonspunkt = AksjonspunktDtoMapper.lagAksjonspunktDto(behandling, getBehandlingsresultat(behandling.getId()),
            totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling.getId()));
        dto.setAksjonspunkt(aksjonspunkt);

        // FIXME hvorfor ytelsspesifikk url her?  Bør kun ha en tilrettelegging url
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
            dto.leggTil(get(SvangerskapspengerRestTjeneste.TILRETTELEGGING_V2_PATH, "svangerskapspenger-tilrettelegging", uuidDto));
        }

        if (BehandlingType.INNSYN.equals(behandling.getType())) {
            return utvideBehandlingDtoForInnsyn(behandling, dto);
        }
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return utvideBehandlingDtoKlage(behandling, dto);
        }
        if (BehandlingType.ANKE.equals(behandling.getType())) {
            return utvideBehandlingDtoAnke(behandling, dto);
        }
        return utvideBehandlingDto(behandling, dto);
    }

    private UtvidetBehandlingDto utvideBehandlingDtoKlage(Behandling behandling, UtvidetBehandlingDto dto) {
        var uuidDto = new UuidDto(behandling.getUuid());
        dto.leggTil(get(KlageRestTjeneste.KLAGE_V2_PATH, "klage-vurdering", uuidDto));
        dto.leggTil(get(KlageRestTjeneste.MOTTATT_KLAGEDOKUMENT_V2_PATH, "mottatt-klagedokument", uuidDto));
        if (behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_VERGE)) {
            dto.leggTil(get(PersonRestTjeneste.VERGE_PATH, "soeker-verge", uuidDto));
            dto.leggTil(get(PersonRestTjeneste.VERGE_BACKEND_PATH, "verge-backend", uuidDto));
        }
        return dto;
    }

    private UtvidetBehandlingDto utvideBehandlingDtoAnke(Behandling behandling, UtvidetBehandlingDto dto) {
        var uuidDto = new UuidDto(behandling.getUuid());
        dto.leggTil(get(AnkeRestTjeneste.ANKEVURDERING_V2_PATH, "anke-vurdering", uuidDto));
        if (behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_VERGE)) {
            dto.leggTil(get(PersonRestTjeneste.VERGE_PATH, "soeker-verge", uuidDto));
            dto.leggTil(get(PersonRestTjeneste.VERGE_BACKEND_PATH, "verge-backend", uuidDto));
        }
        return dto;
    }

    private UtvidetBehandlingDto utvideBehandlingDtoForInnsyn(Behandling behandling, UtvidetBehandlingDto dto) {
        var uuidDto = new UuidDto(behandling.getUuid());
        dto.leggTil(get(InnsynRestTjeneste.INNSYN_PATH, "innsyn", uuidDto));
        dto.leggTil(get(DokumentRestTjeneste.DOKUMENTER_PATH, "dokumenter", uuidDto));
        return dto;
    }

    private UtvidetBehandlingDto utvideBehandlingDto(Behandling behandling, UtvidetBehandlingDto dto) {
        var uuidDto = new UuidDto(behandling.getUuid());
        // mapping ved hjelp av tjenester
        var harInnhentetRegisterData = behandlingRepository.hentSistOppdatertTidspunkt(behandling.getId()).isPresent();
        var harSakenSøknad = søknadRepository.hentSøknadHvisEksisterer(behandling.getId()).isPresent();
        dto.setHarRegisterdata(harInnhentetRegisterData);
        dto.setHarSøknad(harSakenSøknad);
        dto.leggTil(get(SøknadRestTjeneste.SOKNAD_PATH, "soknad", uuidDto));
        dto.leggTil(get(SøknadRestTjeneste.SOKNAD_BACKEND_PATH, "soknad-backend", uuidDto));
        dto.leggTil(get(DokumentRestTjeneste.MOTTATT_DOKUMENTER_PATH, "mottattdokument", uuidDto));

        if (dto.isErAktivPapirsoknad()) {
            return dto;
        }

        dto.leggTil(get(FamiliehendelseRestTjeneste.FAMILIEHENDELSE_V2_PATH, "familiehendelse-v2", uuidDto));
        dto.leggTil(get(PersonRestTjeneste.PERSONOVERSIKT_PATH, "behandling-personoversikt", uuidDto));
        dto.leggTil(get(PersonRestTjeneste.MEDLEMSKAP_V2_PATH, "soeker-medlemskap-v2", uuidDto));

        if (behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_VERGE)) {
            dto.leggTil(get(PersonRestTjeneste.VERGE_PATH, "soeker-verge", uuidDto));
            dto.leggTil(get(PersonRestTjeneste.VERGE_BACKEND_PATH, "verge-backend", uuidDto));
        }

        dto.leggTil(get(InntektArbeidYtelseRestTjeneste.INNTEKT_ARBEID_YTELSE_PATH, "inntekt-arbeid-ytelse", uuidDto));
        dto.leggTil(get(InntektArbeidYtelseRestTjeneste.ARBEIDSGIVERE_OPPLYSNINGER_PATH, "arbeidsgivere-oversikt", uuidDto));
        dto.leggTil(get(InntektArbeidYtelseRestTjeneste.ALLE_INNTEKTSMELDINGER_PATH, "inntektsmeldinger", uuidDto));

        if (behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK)) {
            dto.leggTil(get(OpptjeningRestTjeneste.UTLAND_DOK_STATUS_PATH, "utland-dok-status", uuidDto));
        }

        var harSimuleringAksjonspunkt = behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            var beregning =  Optional.ofNullable(getBehandlingsresultat(behandling.getId()))
                .map(Behandlingsresultat::getBeregningResultat)
                .flatMap(LegacyESBeregningsresultat::getSisteBeregning)
                .isPresent();
            if (beregning || harSimuleringAksjonspunkt) {
                dto.leggTil(get(BeregningsresultatRestTjeneste.ENGANGSTONAD_PATH, "beregningsresultat-engangsstonad", uuidDto));
                dto.setSjekkSimuleringResultat(true);
                dto.leggTil(get(TilbakekrevingRestTjeneste.SIMULERING_PATH, "simulering-resultat", uuidDto));
            }
        } else {
            if (harSimuleringAksjonspunkt || beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId()).isPresent()) {
                dto.setSjekkSimuleringResultat(true);
                dto.leggTil(get(TilbakekrevingRestTjeneste.SIMULERING_PATH, "simulering-resultat", uuidDto));
            }
            dto.leggTil(get(YtelsefordelingRestTjeneste.YTELSESFORDELING_PATH, "ytelsefordeling", uuidDto));
            dto.leggTil(get(OpptjeningRestTjeneste.OPPTJENING_PATH, "opptjening", uuidDto));
            dto.leggTil(get(FeriepengegrunnlagRestTjeneste.FERIEPENGER_PATH, "feriepengegrunnlag", uuidDto));

            var beregningsgrunnlag = beregningTjeneste.hent(behandling.getId())
                    .flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
            if (beregningsgrunnlag.isPresent()) {
                dto.leggTil(get(BeregningsgrunnlagRestTjeneste.BEREGNINGSGRUNNLAG_PATH, "beregningsgrunnlag", uuidDto));
                dto.leggTil(get(UttakRestTjeneste.FAKTA_ARBEIDSFORHOLD_PATH, "fakta-arbeidsforhold", uuidDto));
                if (beregningsgrunnlag.flatMap(Beregningsgrunnlag::getBesteberegningGrunnlag).isPresent()) {
                    dto.leggTil(get(BeregningsgrunnlagRestTjeneste.BEREGNINGSGRUNNLAG_PATH, "beregningsgrunnlagharbesteberegning", uuidDto));
                }
            }

            if (harInnhentetRegisterData) {
                dto.leggTil(get(ArbeidOgInntektsmeldingRestTjeneste.ARBEID_OG_INNTEKTSMELDING_PATH, "arbeidsforhold-inntektsmelding", uuidDto));
                dto.leggTil(post(ArbeidOgInntektsmeldingRestTjeneste.REGISTRER_ARBEIDSFORHOLD_PATH, "arbeidsforhold-inntektsmelding-registrer",
                    new ManueltArbeidsforholdDto()));
                dto.leggTil(post(ArbeidOgInntektsmeldingRestTjeneste.LAGRE_VURDERING_PATH, "arbeidsforhold-inntektsmelding-vurder", new ManglendeOpplysningerVurderingDto()));
                dto.leggTil(
                    post(ArbeidOgInntektsmeldingRestTjeneste.ÅPNE_FOR_NY_VURDERING_PATH, "arbeidsforhold-inntektsmelding-apne-for-ny-vurdering", new BehandlingIdVersjonDto()));
            } else if (SpesialBehandling.erSpesialBehandling(behandling)) {
                dto.leggTil(get(ArbeidOgInntektsmeldingRestTjeneste.ARBEID_OG_INNTEKTSMELDING_PATH, "arbeidsforhold-inntektsmelding", uuidDto));
            }

            var tilkjentYtelse = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId())
                .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of()).stream()
                .anyMatch(p -> p.getDagsats() > 0);
            if (tilkjentYtelse) {
                dto.leggTil(get(BeregningsresultatRestTjeneste.DAGYTELSE_PATH, "beregningsresultat-dagytelse", uuidDto));
            }

            if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
                var svangerskapspengerUttakResultatEntitet = svangerskapspengerUttakResultatRepository.hentHvisEksisterer(behandling.getId());
                var stønadskontoberegning = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(behandling.getFagsak())
                    .flatMap(FagsakRelasjon::getGjeldendeStønadskontoberegning);
                if (stønadskontoberegning.isPresent() && svangerskapspengerUttakResultatEntitet.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.STONADSKONTOER_PATH, "uttak-stonadskontoer", uuidDto));
                }

                if (svangerskapspengerUttakResultatEntitet.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.RESULTAT_SVANGERSKAPSPENGER_PATH, "uttaksresultat-svangerskapspenger", uuidDto));
                }
            } else {
                var yfAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId());
                var harSattEndringsdato = yfAggregat
                    .flatMap(YtelseFordelingAggregat::getAvklarteDatoer).map(AvklarteUttakDatoerEntitet::getGjeldendeEndringsdato).isPresent();
                dto.setHarSattEndringsdato(harSattEndringsdato);
                if (yfAggregat.isPresent()) {
                    if (!dokumentasjonVurderingBehovDtoTjeneste.lagDtos(uuidDto).isEmpty()) {
                        dto.leggTil(get(UttakRestTjeneste.VURDER_DOKUMENTASJON_PATH, "uttak-vurder-dokumentasjon", uuidDto));
                    }
                    if (!faktaUttakPeriodeDtoTjeneste.lagDtos(uuidDto).isEmpty() || behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.FAKTA_UTTAK_INGEN_PERIODER)) {
                        dto.leggTil(get(UttakRestTjeneste.FAKTA_UTTAK_PATH, "uttak-kontroller-fakta-perioder-v2", uuidDto));
                    }
                }
                var uttakResultat = foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandling.getId());
                var stønadskontoberegning = fagsakRelasjonTjeneste.finnRelasjonForHvisEksisterer(behandling.getFagsak())
                    .flatMap(FagsakRelasjon::getGjeldendeStønadskontoberegning);
                if (stønadskontoberegning.isPresent() && uttakResultat.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.STONADSKONTOER_PATH, "uttak-stonadskontoer", uuidDto));
                }

                if (uttakResultat.isPresent()) {
                    var perioder = uttakResultat.map(ForeldrepengerUttak::getGjeldendePerioder).orElse(List.of());
                    dto.setAlleUttaksperioderAvslått(!perioder.isEmpty() && perioder.stream().allMatch(p -> PeriodeResultatType.AVSLÅTT.equals(p.getResultatType())));
                    dto.leggTil(get(UttakRestTjeneste.RESULTAT_PERIODER_PATH, "uttaksresultat-perioder", uuidDto));
                }
            }
        }

        lagTilbakekrevingValgLink(behandling).ifPresent(dto::leggTil);

        behandling.getOriginalBehandlingId().ifPresent(originalBehandlingId -> {
            var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);
            var originalUuidDto = new UuidDto(originalBehandling.getUuid());

            dto.leggTil(get(FamiliehendelseRestTjeneste.FAMILIEHENDELSE_PATH, "familiehendelse-original-behandling", originalUuidDto));
            dto.leggTil(get(SøknadRestTjeneste.SOKNAD_PATH, "soknad-original-behandling", originalUuidDto));

            // FIXME hvorfor ytelsspesifikke urler her?  Bør kun ha en beregningresultat
            if (FagsakYtelseType.ENGANGSTØNAD.equals(originalBehandling.getFagsakYtelseType())) {
                dto.leggTil(
                    get(BeregningsresultatRestTjeneste.ENGANGSTONAD_PATH, "beregningsresultat-engangsstonad-original-behandling",
                        originalUuidDto));
            } else {
                var uttak = foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(originalBehandling.getId());
                if (uttak.isPresent()) {
                    dto.leggTil(get(BeregningsresultatRestTjeneste.DAGYTELSE_PATH, "beregningsresultat-dagytelse-original-behandling", originalUuidDto));
                }
            }
        });

        return dto;
    }

    private Optional<BehandlingsresultatDto> lagBehandlingsresultatDto(Behandling behandling) {
        var behandlingsresultat = getBehandlingsresultat(behandling.getId());
        if (behandlingsresultat == null) {
            return Optional.empty();
        }
        var dto = new BehandlingsresultatDto();
        dto.setId(behandlingsresultat.getId());
        dto.setType(behandlingsresultat.getBehandlingResultatType());
        dto.setAvslagsarsak(behandlingsresultat.getAvslagsårsak());
        dto.setKonsekvenserForYtelsen(behandlingsresultat.getKonsekvenserForYtelsen());
        dto.setRettenTil(behandlingsresultat.getRettenTil());
        dto.setSkjæringstidspunkt(finnSkjæringstidspunktForBehandling(behandling, behandlingsresultat).orElse(null));
        dto.setErRevurderingMedUendretUtfall(erRevurderingMedUendretUtfall(behandling));

        var behandlingDokument = behandlingDokumentRepository.hentHvisEksisterer(behandling.getId());
        if (behandlingDokument.isPresent()) {
            dto.setAvslagsarsakFritekst(behandlingDokument.get().getVedtakFritekst());
            dto.setOverskrift(behandlingDokument.get().getOverstyrtBrevOverskrift());
            dto.setFritekstbrev(behandlingDokument.get().getOverstyrtBrevFritekst());
        }

        dto.setVedtaksbrev(behandlingsresultat.getVedtaksbrev());
        return Optional.of(dto);
    }

    private boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, behandling.getFagsakYtelseType()).orElseThrow().erRevurderingMedUendretUtfall(behandling);
    }

    private Optional<SkjæringstidspunktDto> finnSkjæringstidspunktForBehandling(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        if (!behandling.erYtelseBehandling() || behandlingsresultat.isBehandlingHenlagt()) {
            return Optional.empty();
        }
        return SkjæringstidspunktDto.fraSkjæringstidspunkt(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
    }


    private Optional<ResourceLink> lagTilbakekrevingValgLink(Behandling behandling) {
        var uuidDto = new UuidDto(behandling.getUuid());
        return tilbakekrevingRepository.hent(behandling.getId()).isPresent()
            ? Optional.of(get(TilbakekrevingRestTjeneste.VALG_PATH, "tilbakekrevingvalg", uuidDto))
            : Optional.empty();
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }
}
