package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import static no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoUtil.get;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoUtil.post;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling.BehandlingDtoUtil.setStandardfelter;
import static no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt.AbstractVedtaksbrevOverstyringshåndterer.FPSAK_LAGRE_FRITEKST_INN_FORMIDLING;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.finn.unleash.Unleash;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.BehandlingIdVersjonDto;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
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
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.dokumentbestiller.klient.FormidlingDataTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.klient.TekstFraSaksbehandler;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.familiehendelse.rest.FamiliehendelseRestTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BekreftedeAksjonspunkterDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.anke.AnkeRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.anke.aksjonspunkt.AnkeVurderingResultatAksjonspunktMellomlagringDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold.InntektArbeidYtelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.BeregningsgrunnlagRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.AsyncPollingStatus;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.ByttBehandlendeEnhetDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.GjenopptaBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.HenleggBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.ReåpneBehandlingDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.SettBehandlingPaVentDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.innsyn.InnsynRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.KlageRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt.KlageVurderingResultatAksjonspunktMellomlagringDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.KontrollRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.opptjening.OpptjeningRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.svp.SvangerskapspengerRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.UttakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BehandlingMedUttaksperioderDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.TotrinnskontrollRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.verge.VergeRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.VilkårRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling.YtelsefordelingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;

/**
 * Bygger et sammensatt resultat av BehandlingDto ved å samle data fra ulike tjenester, for å kunne levere dette ut på en REST tjeneste.
 */

//FIXME (TEAM SVP) denne burde splittes til å støtte en enkelt ytelse
@ApplicationScoped
public class BehandlingDtoTjeneste {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private UttakRepository uttakRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private SøknadRepository søknadRepository;
    private Unleash unleash;
    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;
    private BehandlingRepository behandlingRepository;
    private FormidlingDataTjeneste formidlingDataTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    BehandlingDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BehandlingDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                 HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                 TilbakekrevingRepository tilbakekrevingRepository,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                 FormidlingDataTjeneste formidlingDataTjeneste,
                                 Unleash unleash) {

        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.uttakRepository = repositoryProvider.getUttakRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.unleash = unleash;
        this.svangerskapspengerUttakResultatRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.formidlingDataTjeneste = formidlingDataTjeneste;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    }

    private static BehandlingDto lagBehandlingDto(Behandling behandling,
                                                  Optional<BehandlingsresultatDto> behandlingsresultatDto,
                                                  boolean erBehandlingMedGjeldendeVedtak,
                                                  SøknadRepository søknadRepository) {
        BehandlingDto dto = new BehandlingDto();
        UuidDto uuidDto = new UuidDto(behandling.getUuid());
        BehandlingIdDto idDto = new BehandlingIdDto(behandling.getId());
        setStandardfelter(behandling, dto, erBehandlingMedGjeldendeVedtak);
        dto.setSpråkkode(getSpråkkode(behandling, søknadRepository));
        dto.setBehandlingsresultat(behandlingsresultatDto.orElse(null));

        // Felles for alle behandlingstyper
        dto.leggTil(get(BehandlingRestTjeneste.HANDLING_RETTIGHETER_PATH, "handling-rettigheter", uuidDto));

        if (behandling.erYtelseBehandling()) {
            dto.leggTil(get(VergeRestTjeneste.VERGE_BEHANDLINGSMENY_PATH, "finn-menyvalg-for-verge", uuidDto));
        }

        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            dto.leggTil(get(KontrollRestTjeneste.KONTROLLRESULTAT_V2_PATH, "kontrollresultat", idDto));
            dto.leggTil(get(AksjonspunktRestTjeneste.AKSJONSPUNKT_RISIKO_PATH, "risikoklassifisering-aksjonspunkt", uuidDto));
            dto.leggTil(post(AksjonspunktRestTjeneste.AKSJONSPUNKT_PATH, "lagre-risikoklassifisering-aksjonspunkt", new BekreftedeAksjonspunkterDto()));
        }

        if (BehandlingType.REVURDERING.equals(behandling.getType())) {
            dto.leggTil(get(AksjonspunktRestTjeneste.AKSJONSPUNKT_KONTROLLER_REVURDERING_PATH, "har-apent-kontroller-revurdering-aksjonspunkt", uuidDto));
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
        dto.leggTil(get(BrevRestTjeneste.MALER_PATH, "brev-maler", uuidDto));
        dto.leggTil(post(BrevRestTjeneste.BREV_BESTILL_PATH, "brev-bestill", new BestillBrevDto()));

        return dto;
    }

    private static Språkkode getSpråkkode(Behandling behandling, SøknadRepository søknadRepository) {
        Optional<SøknadEntitet> søknadOpt = søknadRepository.hentSøknadHvisEksisterer(behandling.getId());
        if (søknadOpt.isPresent()) {
            return søknadOpt.get().getSpråkkode();
        } else {
            return behandling.getFagsak().getNavBruker().getSpråkkode();
        }
    }

    public List<BehandlingDto> lagBehandlingDtoer(List<Behandling> behandlinger) {
        if (behandlinger.isEmpty()) {
            return Collections.emptyList();
        }
        Optional<BehandlingVedtak> gjeldendeVedtak = behandlingVedtakRepository.hentGjeldendeVedtak(behandlinger.get(0).getFagsak());
        Optional<Behandling> behandlingMedGjeldendeVedtak = gjeldendeVedtak.map(bv -> bv.getBehandlingsresultat().getBehandling());
        return behandlinger.stream().map(behandling -> {
            boolean erBehandlingMedGjeldendeVedtak = erBehandlingMedGjeldendeVedtak(behandling, behandlingMedGjeldendeVedtak);
            var behandlingsresultatDto = lagBehandlingsresultatDto(behandling);
            return lagBehandlingDto(behandling, behandlingsresultatDto, erBehandlingMedGjeldendeVedtak, søknadRepository);
        }).collect(Collectors.toList());
    }

    private boolean erBehandlingMedGjeldendeVedtak(Behandling behandling, Optional<Behandling> behandlingMedGjeldendeVedtak) {
        if (behandlingMedGjeldendeVedtak.isEmpty()) {
            return false;
        }
        return Objects.equals(behandlingMedGjeldendeVedtak.get().getId(), behandling.getId());
    }

    public UtvidetBehandlingDto lagUtvidetBehandlingDto(Behandling behandling, AsyncPollingStatus asyncStatus) {
        Optional<Behandling> sisteAvsluttedeIkkeHenlagteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId());
        UtvidetBehandlingDto dto = mapFra(behandling, erBehandlingMedGjeldendeVedtak(behandling, sisteAvsluttedeIkkeHenlagteBehandling));
        if (asyncStatus != null && !asyncStatus.isPending()) {
            dto.setAsyncStatus(asyncStatus);
        }
        return dto;
    }

    public AnnenPartBehandlingDto lagAnnenPartBehandlingDto(Behandling behandling) {
        return AnnenPartBehandlingDto.mapFra(behandling);
    }

    private void settStandardfelterUtvidet(Behandling behandling, UtvidetBehandlingDto dto, boolean erBehandlingMedGjeldendeVedtak) {
        BehandlingDtoUtil.settStandardfelterUtvidet(behandling, dto, erBehandlingMedGjeldendeVedtak);
        dto.setSpråkkode(getSpråkkode(behandling, søknadRepository));
        var behandlingsresultatDto = lagBehandlingsresultatDto(behandling);
        dto.setBehandlingsresultat(behandlingsresultatDto.orElse(null));
    }

    private static void leggTilLenkerForBehandlingsoperasjoner(Behandling behandling, BehandlingDto dto) {
        // Felles for alle behandlingstyper
        dto.leggTil(post(BehandlingRestTjeneste.BYTT_ENHET_PATH, "bytt-behandlende-enhet", new ByttBehandlendeEnhetDto()));
        dto.leggTil(post(BehandlingRestTjeneste.HENLEGG_PATH, "henlegg-behandling", new HenleggBehandlingDto()));
        dto.leggTil(post(BehandlingRestTjeneste.GJENOPPTA_PATH, "gjenoppta-behandling", new GjenopptaBehandlingDto()));
        dto.leggTil(post(BehandlingRestTjeneste.SETT_PA_VENT_PATH, "sett-behandling-pa-vent", new SettBehandlingPaVentDto()));
        dto.leggTil(post(BehandlingRestTjeneste.ENDRE_VENTEFRIST_PATH, "endre-pa-vent", new SettBehandlingPaVentDto()));
        dto.leggTil(post(AksjonspunktRestTjeneste.AKSJONSPUNKT_PATH, "lagre-aksjonspunkter", new BekreftedeAksjonspunkterDto()));

        if (behandling.erYtelseBehandling()) {
            dto.leggTil(post(BehandlingRestTjeneste.OPNE_FOR_ENDRINGER_PATH, "opne-for-endringer", new ReåpneBehandlingDto()));
            dto.leggTil(post(AksjonspunktRestTjeneste.AKSJONSPUNKT_OVERSTYR_PATH, "lagre-overstyr-aksjonspunkter", new BekreftedeAksjonspunkterDto()));
            dto.leggTil(post(VergeRestTjeneste.VERGE_OPPRETT_PATH, "opprett-verge", new BehandlingIdVersjonDto()));
            dto.leggTil(post(VergeRestTjeneste.VERGE_FJERN_PATH, "fjern-verge", new BehandlingIdVersjonDto()));
            if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
                dto.leggTil(post(UttakRestTjeneste.STONADSKONTOER_GITT_UTTAKSPERIODER_PATH, "lagre-stonadskontoer-gitt-uttaksperioder", new BehandlingMedUttaksperioderDto()));
            }
        }
        if (BehandlingType.ANKE.equals(behandling.getType())) {
            dto.leggTil(post(AnkeRestTjeneste.MELLOMLAGRE_ANKE_PATH, "mellomlagre-anke", new AnkeVurderingResultatAksjonspunktMellomlagringDto()));
            dto.leggTil(post(AnkeRestTjeneste.MELLOMLAGRE_GJENAPNE_ANKE_PATH, "mellomlagre-gjennapne-anke", new AnkeVurderingResultatAksjonspunktMellomlagringDto()));
        }
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            dto.leggTil(post(KlageRestTjeneste.MELLOMLAGRE_PATH, "mellomlagre-klage", new KlageVurderingResultatAksjonspunktMellomlagringDto()));
            dto.leggTil(post(KlageRestTjeneste.MELLOMLAGRE_GJENAPNE_KLAGE_PATH, "mellomlagre-gjennapne-klage", new KlageVurderingResultatAksjonspunktMellomlagringDto()));
        }
    }

    private UtvidetBehandlingDto mapFra(Behandling behandling, boolean erBehandlingMedGjeldendeVedtak) {
        UtvidetBehandlingDto dto = new UtvidetBehandlingDto();
        settStandardfelterUtvidet(behandling, dto, erBehandlingMedGjeldendeVedtak);

        SaksnummerDto saksnummerDto = new SaksnummerDto(behandling.getFagsak().getSaksnummer());
        dto.leggTil(get(FagsakRestTjeneste.FAGSAK_PATH, "fagsak", saksnummerDto));

        leggTilLenkerForBehandlingsoperasjoner(behandling, dto);

        UuidDto uuidDto = new UuidDto(behandling.getUuid());
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
        } else if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return utvideBehandlingDtoKlage(behandling, dto);
        } else if (BehandlingType.ANKE.equals(behandling.getType())) {
            return utvideBehandlingDtoAnke(behandling, dto);
        }
        return utvideBehandlingDto(behandling, dto);
    }

    private UtvidetBehandlingDto utvideBehandlingDtoKlage(Behandling behandling, UtvidetBehandlingDto dto) {
        UuidDto uuidDto = new UuidDto(behandling.getUuid());
        dto.leggTil(get(KlageRestTjeneste.KLAGE_V2_PATH, "klage-vurdering", uuidDto));
        dto.leggTil(get(KlageRestTjeneste.MOTTATT_KLAGEDOKUMENT_V2_PATH, "mottatt-klagedokument", uuidDto));
        return dto;
    }

    private UtvidetBehandlingDto utvideBehandlingDtoAnke(Behandling behandling, UtvidetBehandlingDto dto) {
        UuidDto uuidDto = new UuidDto(behandling.getUuid());
        dto.leggTil(get(AnkeRestTjeneste.ANKEVURDERING_V2_PATH, "anke-vurdering", uuidDto));
        return dto;
    }

    private UtvidetBehandlingDto utvideBehandlingDtoForInnsyn(Behandling behandling, UtvidetBehandlingDto dto) {
        UuidDto uuidDto = new UuidDto(behandling.getUuid());
        dto.leggTil(get(InnsynRestTjeneste.INNSYN_PATH, "innsyn", uuidDto));
        dto.leggTil(get(DokumentRestTjeneste.DOKUMENTER_PATH, "dokumenter", uuidDto));
        return dto;
    }

    private UtvidetBehandlingDto utvideBehandlingDto(Behandling behandling, UtvidetBehandlingDto dto) {
        UuidDto uuidDto = new UuidDto(behandling.getUuid());
        // mapping ved hjelp av tjenester
        dto.leggTil(get(SøknadRestTjeneste.SOKNAD_PATH, "soknad", uuidDto));
        dto.leggTil(get(DokumentRestTjeneste.MOTTATT_DOKUMENTER_PATH, "mottattdokument", uuidDto));

        if (dto.isErAktivPapirsoknad()) {
            return dto;
        }

        dto.leggTil(get(FamiliehendelseRestTjeneste.FAMILIEHENDELSE_PATH, "familiehendelse", uuidDto));
        dto.leggTil(get(FamiliehendelseRestTjeneste.FAMILIEHENDELSE_V2_PATH, "familiehendelse-v2", uuidDto));
        dto.leggTil(get(PersonRestTjeneste.PERSONOPPLYSNINGER_PATH, "soeker-personopplysninger", uuidDto));


        dto.leggTil(get(PersonRestTjeneste.MEDLEMSKAP_V2_PATH, "soeker-medlemskap-v2", uuidDto));
        //TODO (TOR) Legg til else her når frontend ikkje lenger feilaktig brukar både ny og gammal versjon
        dto.leggTil(get(PersonRestTjeneste.MEDLEMSKAP_PATH, "soeker-medlemskap", uuidDto));

        if (behandlingHarVergeAksjonspunkt(behandling)) {
            dto.leggTil(get(PersonRestTjeneste.VERGE_PATH, "soeker-verge", uuidDto));
        }

        if (BehandlingType.REVURDERING.equals(behandling.getType()) && BehandlingStatus.UTREDES.equals(behandling.getStatus())) {
            dto.leggTil(get(BrevRestTjeneste.VARSEL_REVURDERING_PATH, "sendt-varsel-om-revurdering", uuidDto));
        }

        dto.leggTil(get(InntektArbeidYtelseRestTjeneste.INNTEKT_ARBEID_YTELSE_PATH, "inntekt-arbeid-ytelse", uuidDto));


        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            dto.leggTil(get(BeregningsresultatRestTjeneste.ENGANGSTONAD_PATH, "beregningsresultat-engangsstonad", uuidDto));
        } else {
            dto.leggTil(get(YtelsefordelingRestTjeneste.YTELSESFORDELING_PATH, "ytelsefordeling", uuidDto));
            dto.leggTil(get(OpptjeningRestTjeneste.OPPTJENING_PATH, "opptjening", uuidDto));

            Optional<BeregningsgrunnlagEntitet> beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(behandling.getId())
                    .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
            if (beregningsgrunnlag.isPresent()) {
                dto.leggTil(get(BeregningsgrunnlagRestTjeneste.BEREGNINGSGRUNNLAG_PATH, "beregningsgrunnlag", uuidDto));
                dto.leggTil(get(UttakRestTjeneste.FAKTA_ARBEIDSFORHOLD_PATH, "fakta-arbeidsforhold", uuidDto));
            }

            dto.leggTil(get(UttakRestTjeneste.PERIODE_GRENSE_PATH, "uttak-periode-grense", uuidDto));
            dto.leggTil(get(UttakRestTjeneste.KONTROLLER_FAKTA_PERIODER_PATH, "uttak-kontroller-fakta-perioder", uuidDto));

            if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
                Optional<SvangerskapspengerUttakResultatEntitet> svangerskapspengerUttakResultatEntitet = svangerskapspengerUttakResultatRepository.hentHvisEksisterer(behandling.getId());
                Optional<Stønadskontoberegning> stønadskontoberegning = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak())
                    .flatMap(FagsakRelasjon::getGjeldendeStønadskontoberegning);
                if (stønadskontoberegning.isPresent() && svangerskapspengerUttakResultatEntitet.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.STONADSKONTOER_PATH, "uttak-stonadskontoer", uuidDto));
                }

                if (svangerskapspengerUttakResultatEntitet.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.RESULTAT_SVANGERSKAPSPENGER_PATH, "uttaksresultat-svangerskapspenger", uuidDto));
                    dto.leggTil(get(BeregningsresultatRestTjeneste.FORELDREPENGER_PATH, "beregningsresultat-foreldrepenger", uuidDto));
                }
            } else {
                Optional<UttakResultatEntitet> uttakResultat = uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
                Optional<Stønadskontoberegning> stønadskontoberegning = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak())
                    .flatMap(FagsakRelasjon::getGjeldendeStønadskontoberegning);
                if (stønadskontoberegning.isPresent() && uttakResultat.isPresent()) {
                    dto.leggTil(get(UttakRestTjeneste.STONADSKONTOER_PATH, "uttak-stonadskontoer", uuidDto));
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

        behandling.getOriginalBehandling().ifPresent(originalBehandling -> {
            UuidDto originalUuidDto = new UuidDto(originalBehandling.getUuid());
            dto.leggTil(get(FamiliehendelseRestTjeneste.FAMILIEHENDELSE_PATH, "familiehendelse-original-behandling", originalUuidDto));
            dto.leggTil(get(SøknadRestTjeneste.SOKNAD_PATH, "soknad-original-behandling", originalUuidDto));
            Optional<UttakResultatEntitet> uttakResultatHvisEksisterer = uttakRepository.hentUttakResultatHvisEksisterer(originalBehandling.getId());

            // FIXME hvorfor ytelsspesifikke urler her?  Bør kun ha en beregningresultat
            if (FagsakYtelseType.ENGANGSTØNAD.equals(originalBehandling.getFagsakYtelseType())) {
                dto.leggTil(get(BeregningsresultatRestTjeneste.ENGANGSTONAD_PATH, "beregningsresultat-engangsstonad-original-behandling", originalUuidDto));
            } else if (uttakResultatHvisEksisterer.isPresent()) {
                dto.leggTil(get(BeregningsresultatRestTjeneste.FORELDREPENGER_PATH, "beregningsresultat-foreldrepenger-original-behandling", originalUuidDto));
            }
        });

        return dto;
    }

    private Optional<BehandlingsresultatDto> lagBehandlingsresultatDto(Behandling behandling) {
        var behandlingsresultat = behandling.getBehandlingsresultat();
        if (behandlingsresultat == null) {
            return Optional.empty();
        }
        BehandlingsresultatDto dto = new BehandlingsresultatDto();
        Optional<TekstFraSaksbehandler> tekstFraSaksbehandlerOptional = Optional.empty();
            dto.setId(behandlingsresultat.getId());
            dto.setType(behandlingsresultat.getBehandlingResultatType());
            dto.setAvslagsarsak(behandlingsresultat.getAvslagsårsak());
            dto.setKonsekvenserForYtelsen(behandlingsresultat.getKonsekvenserForYtelsen());
            dto.setRettenTil(behandlingsresultat.getRettenTil());
            dto.setSkjæringstidspunkt(finnSkjæringstidspunktForBehandling(behandling).orElse(null));
            dto.setErRevurderingMedUendretUtfall(erRevurderingMedUendretUtfall(behandling));
            if (unleash.isEnabled(FPSAK_LAGRE_FRITEKST_INN_FORMIDLING)) {
                tekstFraSaksbehandlerOptional = formidlingDataTjeneste.hentSaksbehandlerTekst(behandling.getUuid());
            }
            //For å støtte eksiterende data inn i FPSAK
            if (tekstFraSaksbehandlerOptional.isPresent()) {
                final TekstFraSaksbehandler tekstFraSaksbehandlerDto = tekstFraSaksbehandlerOptional.get();
                dto.setAvslagsarsakFritekst(tekstFraSaksbehandlerDto.getAvslagarsakFritekst());
                dto.setVedtaksbrev(tekstFraSaksbehandlerDto.getVedtaksbrev());
                dto.setOverskrift(tekstFraSaksbehandlerDto.getOverskrift());
                dto.setFritekstbrev(tekstFraSaksbehandlerDto.getFritekstbrev());
            } else {
                dto.setAvslagsarsakFritekst(behandlingsresultat.getAvslagarsakFritekst());
                dto.setVedtaksbrev(behandlingsresultat.getVedtaksbrev());
                dto.setOverskrift(behandlingsresultat.getOverskrift());
                dto.setFritekstbrev(behandlingsresultat.getFritekstbrev());
            }
            return Optional.of(dto);
    }

    private boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, behandling.getFagsakYtelseType()).orElseThrow().erRevurderingMedUendretUtfall(behandling);
    }

    private Optional<SkjæringstidspunktDto> finnSkjæringstidspunktForBehandling(Behandling behandling) {
        if (!behandling.erYtelseBehandling()) {
            return Optional.empty();
        }
        Optional<LocalDate> skjæringstidspunktHvisUtledet = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getSkjæringstidspunktHvisUtledet();
        if (skjæringstidspunktHvisUtledet.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SkjæringstidspunktDto(skjæringstidspunktHvisUtledet.get()));
    }

    private LocalDate finnSkjæringstidspunktForSøknad(Behandling behandling) {
        if (!behandling.erYtelseBehandling()) {
            return null;
        }
        return skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getSkjæringstidspunktHvisUtledet().orElse(null);
    }

    private Optional<ResourceLink> lagSimuleringResultatLink(Behandling behandling) {
        //fpoppdrag.override.proxy.url brukes ved testing lokalt
        BehandlingIdDto idDto = new BehandlingIdDto(behandling.getId());
        String fpoppdragOverrideUrl = System.getProperty("fpoppdrag.override.proxy.url");
        String baseUurl = fpoppdragOverrideUrl != null ? fpoppdragOverrideUrl : "/fpoppdrag/api";
        return Optional.of(ResourceLink.post(baseUurl + "/simulering/resultat-uten-inntrekk", "simuleringResultat", idDto));
    }

    private Optional<ResourceLink> lagTilbakekrevingValgLink(Behandling behandling) {
        var uuidDto = new UuidDto(behandling.getUuid());
        // FIXME (BehandlingIdDto): bør kunne støtte behandlingUuid også
        return tilbakekrevingRepository.hent(behandling.getId()).isPresent()
            ? Optional.of(get(TilbakekrevingRestTjeneste.VALG_PATH, "tilbakekrevingvalg", uuidDto))
            : Optional.empty();
    }

    private boolean behandlingHarVergeAksjonspunkt(Behandling behandling) {
        return behandling.getAksjonspunkter().stream()
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .anyMatch(AksjonspunktDefinisjon.AVKLAR_VERGE::equals);
    }

    public Boolean finnBehandlingOperasjonRettigheter(Behandling behandling) {
        return this.søknadRepository.hentSøknadHvisEksisterer(behandling.getId()).isPresent();
    }
}
