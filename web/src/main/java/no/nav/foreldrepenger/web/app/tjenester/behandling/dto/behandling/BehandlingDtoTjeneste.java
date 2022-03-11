package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ManglendeOpplysningerVurderingDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ManueltArbeidsforholdDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdInntektsmeldingToggleTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.OpptjeningIUtlandDokStatusTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.rest.ResourceLinks;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjenestePathHack1;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BekreftedeAksjonspunkterDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.anke.AnkeRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt.AnkeVurderingResultatAksjonspunktMellomlagringDto;
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
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageVurderingResultatAksjonspunktMellomlagringDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.SendTilKabalDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.KontrollRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.opptjening.OpptjeningRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.svp.SvangerskapspengerRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.UttakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.KontrollerAktivitetskravDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BehandlingMedUttaksperioderDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.TotrinnskontrollRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.verge.VergeRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.VilkårRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.YtelsefordelingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.familiehendelse.FamiliehendelseRestTjeneste;

import static no.nav.foreldrepenger.web.app.rest.ResourceLinks.*;

/**
 * Bygger et sammensatt resultat av BehandlingDto ved å samle data fra ulike tjenester, for å kunne levere dette ut på en REST tjeneste.
 */

//FIXME (TEAM SVP) denne burde splittes til å støtte en enkelt ytelse
@ApplicationScoped
public class BehandlingDtoTjeneste {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private SøknadRepository søknadRepository;
    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private OpptjeningIUtlandDokStatusTjeneste opptjeningIUtlandDokStatusTjeneste;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    private String fpoppdragOverrideProxyUrl;
    private KontrollerAktivitetskravDtoTjeneste kontrollerAktivitetskravDtoTjeneste;

    @Inject
    public BehandlingDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                 HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                 TilbakekrevingRepository tilbakekrevingRepository,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                 OpptjeningIUtlandDokStatusTjeneste opptjeningIUtlandDokStatusTjeneste,
                                 BehandlingDokumentRepository behandlingDokumentRepository,
                                 RelatertBehandlingTjeneste relatertBehandlingTjeneste,
                                 ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste,
                                 @KonfigVerdi(value = "fpoppdrag.override.proxy.url", required = false) String fpoppdragOverrideProxyUrl,
                                 KontrollerAktivitetskravDtoTjeneste kontrollerAktivitetskravDtoTjeneste) {

        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.foreldrepengerUttakTjeneste = foreldrepengerUttakTjeneste;
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.svangerskapspengerUttakResultatRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.opptjeningIUtlandDokStatusTjeneste = opptjeningIUtlandDokStatusTjeneste;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
        this.fpoppdragOverrideProxyUrl = fpoppdragOverrideProxyUrl;
        this.kontrollerAktivitetskravDtoTjeneste = kontrollerAktivitetskravDtoTjeneste;
    }

    BehandlingDtoTjeneste() {
        // for CDI proxy
    }

    private BehandlingDto lagBehandlingDto(Behandling behandling,
                                           Optional<BehandlingsresultatDto> behandlingsresultatDto,
                                           boolean erBehandlingMedGjeldendeVedtak,
                                           SøknadRepository søknadRepository,
                                           LocalDate vedtaksdato,
                                           BehandlingRepository behandlingRepository) {
        var dto = new BehandlingDto();
        var uuidDto = new UuidDto(behandling.getUuid());
        BehandlingDtoUtil.setStandardfelterMedGjeldendeVedtak(behandling, dto, erBehandlingMedGjeldendeVedtak, vedtaksdato);
        dto.setSpråkkode(getSpråkkode(behandling, søknadRepository, behandlingRepository));
        dto.setBehandlingsresultat(behandlingsresultatDto.orElse(null));

        // Felles for alle behandlingstyper
        dto.leggTil(get(BehandlingRestTjeneste.RETTIGHETER_PATH, "behandling-rettigheter", uuidDto));

        if (behandling.erYtelseBehandling()) {
            dto.leggTil(get(PersonRestTjeneste.PERSONOVERSIKT_PATH, "behandling-personoversikt", uuidDto));
            dto.leggTil(get(FamiliehendelseRestTjeneste.FAMILIEHENDELSE_V2_PATH, "familiehendelse-v2", uuidDto));
        }

        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            dto.leggTil(get(KontrollRestTjeneste.KONTROLLRESULTAT_V2_PATH, "kontrollresultat", uuidDto));
            dto.leggTil(get(AksjonspunktRestTjeneste.AKSJONSPUNKT_RISIKO_PATH, "risikoklassifisering-aksjonspunkt", uuidDto));
        }

        if (BehandlingType.REVURDERING.equals(behandling.getType())) {
            dto.leggTil(
                get(AksjonspunktRestTjeneste.AKSJONSPUNKT_KONTROLLER_REVURDERING_PATH, "har-apent-kontroller-revurdering-aksjonspunkt",
                    uuidDto));
            dto.leggTil(get(BeregningsresultatRestTjeneste.HAR_SAMME_RESULTAT_PATH, "har-samme-resultat", uuidDto));
        }

        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            dto.leggTil(get(KlageRestTjeneste.KLAGE_V2_PATH, "klage-vurdering", uuidDto));
        }

        if (!BehandlingType.INNSYN.equals(behandling.getType())) {
            // Totrinnsbehandling
            if (BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus())) {
                dto.leggTil(get(TotrinnskontrollRestTjeneste.ARSAKER_PATH, "totrinnskontroll-arsaker", uuidDto));
                dto.leggTil(post(AksjonspunktRestTjeneste.AKSJONSPUNKT_PATH, "bekreft-totrinnsaksjonspunkt", uuidDto));
            } else if (BehandlingStatus.UTREDES.equals(behandling.getStatus())) {
                dto.leggTil(get(TotrinnskontrollRestTjeneste.ARSAKER_READ_ONLY_PATH, "totrinnskontroll-arsaker-readOnly", uuidDto));
            }
        }

        // Brev
        dto.leggTil(ResourceLink.get("/fpformidling/api/brev/maler", "brev-maler", uuidDto));
        dto.leggTil(post(BrevRestTjeneste.BREV_BESTILL_PATH, "brev-bestill", new BestillBrevDto()));

        return dto;
    }

    private static Språkkode getSpråkkode(Behandling behandling, SøknadRepository søknadRepository, BehandlingRepository behandlingRepository) {
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
            var vedtaksdato = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId()).map(BehandlingVedtak::getVedtaksdato)
                .orElse(null);
            return lagBehandlingDto(behandling, behandlingsresultatDto, erBehandlingMedGjeldendeVedtak,
                søknadRepository, vedtaksdato, behandlingRepository);
        }).collect(Collectors.toList());
    }

    private boolean erBehandlingMedGjeldendeVedtak(Behandling behandling, Optional<Behandling> behandlingMedGjeldendeVedtak) {
        if (behandlingMedGjeldendeVedtak.isEmpty()) {
            return false;
        }
        return Objects.equals(behandlingMedGjeldendeVedtak.get().getId(), behandling.getId());
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
        BehandlingDtoUtil.settStandardfelterUtvidet(behandling, dto, erBehandlingMedGjeldendeVedtak, vedtaksDato);
        dto.setSpråkkode(getSpråkkode(behandling, søknadRepository, behandlingRepository));
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

        if (behandling.erYtelseBehandling()) {
            dto.leggTil(post(BehandlingRestTjeneste.OPNE_FOR_ENDRINGER_PATH, "opne-for-endringer", new ReåpneBehandlingDto()));
            dto.leggTil(post(AksjonspunktRestTjeneste.AKSJONSPUNKT_OVERSTYR_PATH, "lagre-overstyr-aksjonspunkter",
                new BekreftedeAksjonspunkterDto()));
            dto.leggTil(post(VergeRestTjeneste.VERGE_OPPRETT_PATH, "opprett-verge", new BehandlingIdVersjonDto()));
            dto.leggTil(post(VergeRestTjeneste.VERGE_FJERN_PATH, "fjern-verge", new BehandlingIdVersjonDto()));
            if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
                dto.leggTil(post(UttakRestTjeneste.STONADSKONTOER_GITT_UTTAKSPERIODER_PATH, "lagre-stonadskontoer-gitt-uttaksperioder",
                    new BehandlingMedUttaksperioderDto()));
            }
        }
        if (BehandlingType.ANKE.equals(behandling.getType())) {
            dto.leggTil(post(AnkeRestTjeneste.MELLOMLAGRE_ANKE_PATH, "mellomlagre-anke", new AnkeVurderingResultatAksjonspunktMellomlagringDto()));
        }
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            dto.leggTil(post(KlageRestTjeneste.MELLOMLAGRE_PATH, "mellomlagre-klage", new KlageVurderingResultatAksjonspunktMellomlagringDto()));
            dto.leggTil(post(KlageRestTjeneste.KABAL_PATH, "kabal-klage", new SendTilKabalDto(behandling.getUuid(), null)));
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
        if (!dto.isErAktivPapirsoknad()) {
            dto.leggTil(get(VilkårRestTjeneste.VILKÅR_V2_PATH, "vilkar", uuidDto));
        }
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
        return dto;
    }

    private UtvidetBehandlingDto utvideBehandlingDtoAnke(Behandling behandling, UtvidetBehandlingDto dto) {
        var uuidDto = new UuidDto(behandling.getUuid());
        dto.leggTil(get(AnkeRestTjeneste.ANKEVURDERING_V2_PATH, "anke-vurdering", uuidDto));
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
        dto.leggTil(get(SøknadRestTjeneste.SOKNAD_PATH, "soknad", uuidDto));
        dto.leggTil(get(SøknadRestTjeneste.SOKNAD_BACKEND_PATH, "soknad-backend", uuidDto));
        dto.leggTil(get(DokumentRestTjeneste.MOTTATT_DOKUMENTER_PATH, "mottattdokument", uuidDto));

        if (dto.isErAktivPapirsoknad()) {
            return dto;
        }

        dto.leggTil(get(FamiliehendelseRestTjeneste.FAMILIEHENDELSE_V2_PATH, "familiehendelse-v2", uuidDto));
        dto.leggTil(get(PersonRestTjeneste.PERSONOVERSIKT_PATH, "behandling-personoversikt", uuidDto));
        dto.leggTil(get(PersonRestTjeneste.MEDLEMSKAP_V2_PATH, "soeker-medlemskap-v2", uuidDto));

        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            dto.leggTil(get(KontrollRestTjeneste.KONTROLLRESULTAT_V2_PATH, "kontrollresultat", uuidDto));
        }

        if (behandlingHarVergeAksjonspunkt(behandling)) {
            dto.leggTil(get(PersonRestTjeneste.VERGE_PATH, "soeker-verge", uuidDto));
            dto.leggTil(get(PersonRestTjeneste.VERGE_BACKEND_PATH, "verge-backend", uuidDto));
        }

        dto.leggTil(get(InntektArbeidYtelseRestTjeneste.INNTEKT_ARBEID_YTELSE_PATH, "inntekt-arbeid-ytelse", uuidDto));
        dto.leggTil(get(InntektArbeidYtelseRestTjeneste.ARBEIDSGIVERE_OPPLYSNINGER_PATH, "arbeidsgivere-oversikt", uuidDto));
        dto.leggTil(get(InntektArbeidYtelseRestTjeneste.ALLE_INNTEKTSMELDINGER_PATH, "inntektsmeldinger", uuidDto));

        if (opptjeningIUtlandDokStatusTjeneste.hentStatus(behandling.getId()).isPresent()) {
            dto.leggTil(get(OpptjeningRestTjeneste.UTLAND_DOK_STATUS_PATH, "utland-dok-status", uuidDto));
        }

        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            dto.leggTil(get(BeregningsresultatRestTjeneste.ENGANGSTONAD_PATH, "beregningsresultat-engangsstonad", uuidDto));
        } else {
            dto.leggTil(get(YtelsefordelingRestTjeneste.YTELSESFORDELING_PATH, "ytelsefordeling", uuidDto));
            dto.leggTil(get(OpptjeningRestTjeneste.OPPTJENING_PATH, "opptjening", uuidDto));
            dto.leggTil(get(FeriepengegrunnlagRestTjeneste.FERIEPENGER_PATH, "feriepengegrunnlag", uuidDto));

            var beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId())
                    .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
            if (beregningsgrunnlag.isPresent()) {
                dto.leggTil(get(BeregningsgrunnlagRestTjeneste.BEREGNINGSGRUNNLAG_PATH, "beregningsgrunnlag", uuidDto));
                dto.leggTil(get(UttakRestTjeneste.FAKTA_ARBEIDSFORHOLD_PATH, "fakta-arbeidsforhold", uuidDto));
            }

            dto.leggTil(get(UttakRestTjeneste.PERIODE_GRENSE_PATH, "uttak-periode-grense", uuidDto));
            dto.leggTil(get(UttakRestTjeneste.KONTROLLER_FAKTA_PERIODER_PATH, "uttak-kontroller-fakta-perioder", uuidDto));

            if (ArbeidsforholdInntektsmeldingToggleTjeneste.erTogglePå()) {
                dto.leggTil(
                    get(ArbeidOgInntektsmeldingRestTjeneste.ARBEID_OG_INNTEKTSMELDING_PATH, "arbeidsforhold-inntektsmelding", uuidDto));
                dto.leggTil(
                    post(ArbeidOgInntektsmeldingRestTjeneste.REGISTRER_ARBEIDSFORHOLD_PATH, "arbeidsforhold-inntektsmelding-registrer",
                        new ManueltArbeidsforholdDto()));
                dto.leggTil(post(ArbeidOgInntektsmeldingRestTjeneste.LAGRE_VURDERING_PATH, "arbeidsforhold-inntektsmelding-vurder",
                    new ManglendeOpplysningerVurderingDto()));
                dto.leggTil(post(ArbeidOgInntektsmeldingRestTjeneste.ÅPNE_FOR_NY_VURDERING_PATH,
                    "arbeidsforhold-inntektsmelding-apne-for-ny-vurdering", new BehandlingIdVersjonDto()));
            }

            if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
                var svangerskapspengerUttakResultatEntitet = svangerskapspengerUttakResultatRepository.hentHvisEksisterer(behandling.getId());
                var stønadskontoberegning = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak())
                    .flatMap(FagsakRelasjon::getGjeldendeStønadskontoberegning);
                if (stønadskontoberegning.isPresent() && svangerskapspengerUttakResultatEntitet.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.STONADSKONTOER_PATH, "uttak-stonadskontoer", uuidDto));
                }

                if (svangerskapspengerUttakResultatEntitet.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.RESULTAT_SVANGERSKAPSPENGER_PATH, "uttaksresultat-svangerskapspenger", uuidDto));
                    dto.leggTil(get(BeregningsresultatRestTjeneste.FORELDREPENGER_PATH, "beregningsresultat-foreldrepenger", uuidDto));
                }
            } else {
                dto.leggTil(get(UttakRestTjeneste.SAMMENHENGENDE_UTTAK_PATH, "krever-sammenhengende-uttak", uuidDto));
                if (!kontrollerAktivitetskravDtoTjeneste.lagDtos(uuidDto).isEmpty()) {
                    dto.leggTil(get(UttakRestTjeneste.KONTROLLER_AKTIVTETSKRAV_PATH, "uttak-kontroller-aktivitetskrav", uuidDto));
                }
                var uttakResultat = foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(behandling.getId());
                var stønadskontoberegning = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak())
                    .flatMap(FagsakRelasjon::getGjeldendeStønadskontoberegning);
                if (stønadskontoberegning.isPresent() && uttakResultat.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.STONADSKONTOER_PATH, "uttak-stonadskontoer", uuidDto));
                }

                var uttakResultatAnnenPart = hentUttakAnnenpartHvisEksisterer(behandling);
                if (uttakResultat.isPresent() || uttakResultatAnnenPart.isPresent()) {
                    // Fpformidling trenger også å få fatt på uttaksresultatet når bare annen part har det
                    dto.leggTil(get(UttakRestTjeneste.RESULTAT_PERIODER_PATH, "uttaksresultat-perioder-formidling", uuidDto));
                }
                if (uttakResultat.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.RESULTAT_PERIODER_PATH, "uttaksresultat-perioder", uuidDto));
                    // FIXME: Bør ikke ha ytelsesspesifikk url her. Bør kun være beregningsresultat
                    dto.leggTil(get(BeregningsresultatRestTjeneste.FORELDREPENGER_PATH, "beregningsresultat-foreldrepenger", uuidDto));
                }
            }
        }

        lagTilbakekrevingValgLink(behandling).ifPresent(dto::leggTil);
        lagSimuleringResultatLink(behandling).ifPresent(dto::leggTil);

        behandling.getOriginalBehandlingId().ifPresent(originalBehandlingId -> {
            var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);
            var originalUuidDto = new UuidDto(originalBehandling.getUuid());

            // Denne brukes kun av FPFORMIDLING
            dto.leggTil(get(BehandlingRestTjenestePathHack1.BEHANDLING_PATH, "original-behandling", originalUuidDto));

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
                    dto.leggTil(
                        get(BeregningsresultatRestTjeneste.FORELDREPENGER_PATH, "beregningsresultat-foreldrepenger-original-behandling",
                            originalUuidDto));
                }
            }
        });

        return dto;
    }

    private Optional<ForeldrepengerUttak> hentUttakAnnenpartHvisEksisterer(Behandling søkersBehandling) {
        var annenpartBehandling = relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(søkersBehandling.getFagsak().getSaksnummer());
        return annenpartBehandling.flatMap(ab -> foreldrepengerUttakTjeneste.hentUttakHvisEksisterer(ab.getId()));
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
        var skjæringstidspunktHvisUtledet = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getSkjæringstidspunktHvisUtledet();
        if (skjæringstidspunktHvisUtledet.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SkjæringstidspunktDto(skjæringstidspunktHvisUtledet.get()));
    }

    private Optional<ResourceLink> lagSimuleringResultatLink(Behandling behandling) {
        //fpoppdrag.override.proxy.url brukes ved testing lokalt og docker-compose
        var baseUurl = fpoppdragOverrideProxyUrl != null ? fpoppdragOverrideProxyUrl : "/fpoppdrag/api";
        return Optional.of(ResourceLink.post(baseUurl + "/simulering/resultat-uten-inntrekk", "simuleringResultat",
            new InternBehandlingIdDto(behandling.getId())));
    }

    private Optional<ResourceLink> lagTilbakekrevingValgLink(Behandling behandling) {
        var uuidDto = new UuidDto(behandling.getUuid());
        return tilbakekrevingRepository.hent(behandling.getId()).isPresent()
            ? Optional.of(get(TilbakekrevingRestTjeneste.VALG_PATH, "tilbakekrevingvalg", uuidDto))
            : Optional.empty();
    }

    private boolean behandlingHarVergeAksjonspunkt(Behandling behandling) {
        return behandling.getAksjonspunkter().stream()
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .anyMatch(AksjonspunktDefinisjon.AVKLAR_VERGE::equals);
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }

    //Ikke bruk denne hvis ikke nødvendig, bruk BehandlingIdDto med UUID
    private record InternBehandlingIdDto(Long behandlingId) {
    }
}
