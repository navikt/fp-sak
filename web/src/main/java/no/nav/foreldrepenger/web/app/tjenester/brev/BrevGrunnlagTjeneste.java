package no.nav.foreldrepenger.web.app.tjenester.brev;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.eøs.EøsUttaksperiodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.uttak.Uttak;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
class BrevGrunnlagTjeneste {

    private SøknadRepository søknadRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private UttakTjeneste uttakTjeneste;
    private MedlemTjeneste medlemTjeneste;
    private EøsUttakRepository eøsUttakRepository;
    private UtregnetStønadskontoTjeneste utregnetStønadskontoTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private UføretrygdRepository uføretrygdRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;

    @Inject
    public BrevGrunnlagTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                FamilieHendelseTjeneste familieHendelseTjeneste,
                                UttakTjeneste uttakTjeneste,
                                MedlemTjeneste medlemTjeneste,
                                EøsUttakRepository eøsUttakRepository,
                                UtregnetStønadskontoTjeneste utregnetStønadskontoTjeneste,
                                YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                UføretrygdRepository uføretrygdRepository,
                                BehandlingDokumentRepository behandlingDokumentRepository,
                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                DekningsgradTjeneste dekningsgradTjeneste) {
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.medlemTjeneste = medlemTjeneste;
        this.eøsUttakRepository = eøsUttakRepository;
        this.utregnetStønadskontoTjeneste = utregnetStønadskontoTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.uføretrygdRepository = uføretrygdRepository;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
    }

    BrevGrunnlagTjeneste() {
        //CDI
    }

    BrevGrunnlagDto toDto(Behandling behandling) {
        var uuid = behandling.getUuid();
        var behandlingType = mapBehandlingType(behandling.getType());

        var opprettet = behandling.getOpprettetDato();
        var avsluttet = behandling.getAvsluttetDato();
        var behandlendeEnhet = behandling.getBehandlendeEnhet();
        var språkkode = finnSpråkkode(behandling);
        var automatiskBehandlet = behandling.isToTrinnsBehandling() || behandlingType == BrevGrunnlagDto.BehandlingType.KLAGE;
        var familieHendelse = finnFamilieHendelse(behandling.getId()).orElse(null);
        var originalBehandlingFamilieHendelse = behandling.getOriginalBehandlingId().flatMap(this::finnFamilieHendelse).orElse(null);
        var rettigheter = utledRettigheter(behandling);
        var behandlingsresultat = finnBehandlingsresultat(behandling).orElse(null);
        var behandlingÅrsakTyper = finnBehandlingÅrsakTyper(behandling);
        return new BrevGrunnlagDto(uuid, behandlingType, opprettet, avsluttet, behandlendeEnhet, språkkode, automatiskBehandlet, familieHendelse,
            originalBehandlingFamilieHendelse, rettigheter, behandlingsresultat, behandlingÅrsakTyper);
    }

    private List<BrevGrunnlagDto.BehandlingÅrsakType> finnBehandlingÅrsakTyper(Behandling behandling) {
        return behandling.getBehandlingÅrsaker().stream().map(BrevGrunnlagTjeneste::mapBehandlingÅrsakType).toList();
    }

    private static BrevGrunnlagDto.BehandlingÅrsakType mapBehandlingÅrsakType(BehandlingÅrsak behandlingÅrsak) {
        return switch (behandlingÅrsak.getBehandlingÅrsakType()) {
            case RE_FEIL_I_LOVANDVENDELSE -> BrevGrunnlagDto.BehandlingÅrsakType.RE_FEIL_I_LOVANDVENDELSE;
            case RE_FEIL_REGELVERKSFORSTÅELSE -> BrevGrunnlagDto.BehandlingÅrsakType.RE_FEIL_REGELVERKSFORSTÅELSE;
            case RE_FEIL_ELLER_ENDRET_FAKTA -> BrevGrunnlagDto.BehandlingÅrsakType.RE_FEIL_ELLER_ENDRET_FAKTA;
            case RE_FEIL_PROSESSUELL -> BrevGrunnlagDto.BehandlingÅrsakType.RE_FEIL_PROSESSUELL;
            case RE_ANNET -> BrevGrunnlagDto.BehandlingÅrsakType.RE_ANNET;
            case RE_OPPLYSNINGER_OM_MEDLEMSKAP -> BrevGrunnlagDto.BehandlingÅrsakType.RE_OPPLYSNINGER_OM_MEDLEMSKAP;
            case RE_OPPLYSNINGER_OM_OPPTJENING -> BrevGrunnlagDto.BehandlingÅrsakType.RE_OPPLYSNINGER_OM_OPPTJENING;
            case RE_OPPLYSNINGER_OM_FORDELING -> BrevGrunnlagDto.BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING;
            case RE_OPPLYSNINGER_OM_INNTEKT -> BrevGrunnlagDto.BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT;
            case RE_OPPLYSNINGER_OM_FØDSEL -> BrevGrunnlagDto.BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FØDSEL;
            case RE_OPPLYSNINGER_OM_DØD -> BrevGrunnlagDto.BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD;
            case RE_OPPLYSNINGER_OM_SØKERS_REL -> BrevGrunnlagDto.BehandlingÅrsakType.RE_OPPLYSNINGER_OM_SØKERS_REL;
            case RE_OPPLYSNINGER_OM_SØKNAD_FRIST -> BrevGrunnlagDto.BehandlingÅrsakType.RE_OPPLYSNINGER_OM_SØKNAD_FRIST;
            case RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG -> BrevGrunnlagDto.BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG;
            case RE_KLAGE_UTEN_END_INNTEKT -> BrevGrunnlagDto.BehandlingÅrsakType.RE_KLAGE_UTEN_END_INNTEKT;
            case RE_KLAGE_MED_END_INNTEKT -> BrevGrunnlagDto.BehandlingÅrsakType.RE_KLAGE_MED_END_INNTEKT;
            case ETTER_KLAGE -> BrevGrunnlagDto.BehandlingÅrsakType.ETTER_KLAGE;
            case RE_MANGLER_FØDSEL -> BrevGrunnlagDto.BehandlingÅrsakType.RE_MANGLER_FØDSEL;
            case RE_MANGLER_FØDSEL_I_PERIODE -> BrevGrunnlagDto.BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE;
            case RE_AVVIK_ANTALL_BARN -> BrevGrunnlagDto.BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN;
            case RE_ENDRING_FRA_BRUKER -> BrevGrunnlagDto.BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
            case RE_ENDRET_INNTEKTSMELDING -> BrevGrunnlagDto.BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
            case BERØRT_BEHANDLING -> BrevGrunnlagDto.BehandlingÅrsakType.BERØRT_BEHANDLING;
            case REBEREGN_FERIEPENGER -> BrevGrunnlagDto.BehandlingÅrsakType.REBEREGN_FERIEPENGER;
            case RE_UTSATT_START -> BrevGrunnlagDto.BehandlingÅrsakType.RE_UTSATT_START;
            case RE_SATS_REGULERING -> BrevGrunnlagDto.BehandlingÅrsakType.RE_SATS_REGULERING;
            case ENDRE_DEKNINGSGRAD -> BrevGrunnlagDto.BehandlingÅrsakType.ENDRE_DEKNINGSGRAD;
            case INFOBREV_BEHANDLING -> BrevGrunnlagDto.BehandlingÅrsakType.INFOBREV_BEHANDLING;
            case INFOBREV_OPPHOLD -> BrevGrunnlagDto.BehandlingÅrsakType.INFOBREV_OPPHOLD;
            case INFOBREV_PÅMINNELSE -> BrevGrunnlagDto.BehandlingÅrsakType.INFOBREV_PÅMINNELSE;
            case OPPHØR_YTELSE_NYTT_BARN -> BrevGrunnlagDto.BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN;
            case RE_HENDELSE_FØDSEL -> BrevGrunnlagDto.BehandlingÅrsakType.RE_HENDELSE_FØDSEL;
            case RE_HENDELSE_DØD_FORELDER -> BrevGrunnlagDto.BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER;
            case RE_HENDELSE_DØD_BARN -> BrevGrunnlagDto.BehandlingÅrsakType.RE_HENDELSE_DØD_BARN;
            case RE_HENDELSE_DØDFØDSEL -> BrevGrunnlagDto.BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL;
            case RE_HENDELSE_UTFLYTTING -> BrevGrunnlagDto.BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING;
            case RE_VEDTAK_PLEIEPENGER -> BrevGrunnlagDto.BehandlingÅrsakType.RE_VEDTAK_PLEIEPENGER;
            case FEIL_PRAKSIS_UTSETTELSE -> BrevGrunnlagDto.BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE;
            case FEIL_PRAKSIS_IVERKS_UTSET -> BrevGrunnlagDto.BehandlingÅrsakType.FEIL_PRAKSIS_IVERKS_UTSET;
            case FEIL_PRAKSIS_BG_AAP_KOMBI -> BrevGrunnlagDto.BehandlingÅrsakType.FEIL_PRAKSIS_BG_AAP_KOMBI;
            case KLAGE_TILBAKEBETALING -> BrevGrunnlagDto.BehandlingÅrsakType.KLAGE_TILBAKEBETALING;
            case RE_OPPLYSNINGER_OM_YTELSER -> BrevGrunnlagDto.BehandlingÅrsakType.RE_OPPLYSNINGER_OM_YTELSER;
            case RE_REGISTEROPPLYSNING -> BrevGrunnlagDto.BehandlingÅrsakType.RE_REGISTEROPPLYSNING;
            case KØET_BEHANDLING -> BrevGrunnlagDto.BehandlingÅrsakType.KØET_BEHANDLING;
            case RE_TILSTØTENDE_YTELSE_INNVILGET -> BrevGrunnlagDto.BehandlingÅrsakType.RE_TILSTØTENDE_YTELSE_INNVILGET;
            case RE_TILSTØTENDE_YTELSE_OPPHØRT -> BrevGrunnlagDto.BehandlingÅrsakType.RE_TILSTØTENDE_YTELSE_OPPHØRT;
            case UDEFINERT -> BrevGrunnlagDto.BehandlingÅrsakType.UDEFINERT;
        };
    }

    private Optional<BrevGrunnlagDto.BehandlingsresultatDto> finnBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).map(behandlingsresultat -> {
            var medlemskapOpphørsårsak = finnMedlemskapOpphørsÅrsak(behandling).orElse(null);
            var medlemskapFom = medlemTjeneste.hentMedlemFomDato(behandling.getId()).orElse(null);
            var behandlingResultatType = mapBehandlingResultatType(behandlingsresultat.getBehandlingResultatType());
            var avslagsårsak = mapAvslagsårsak(behandlingsresultat.getAvslagsårsak());
            var fritekst = finnAvslagsårsakFritekst(behandling).orElse(null);
            var stp = finnSkjæringstidspunktForBehandling(behandling, behandlingsresultat).orElse(null);
            var endretDekningsgrad = dekningsgradTjeneste.behandlingHarEndretDekningsgrad(BehandlingReferanse.fra(behandling));
            var opphørsdato = finnOpphørsdato(behandling).orElse(null);
            var konsekvenserForYtelsen = finnKonsekvenserForYtelsen(behandlingsresultat);
            var vilkårTyper = finnVilkårTyper(behandlingsresultat);
            return new BrevGrunnlagDto.BehandlingsresultatDto(medlemskapOpphørsårsak, medlemskapFom, behandlingResultatType, avslagsårsak, fritekst,
                stp, endretDekningsgrad, opphørsdato, konsekvenserForYtelsen, vilkårTyper);
        });
    }

    private static List<BrevGrunnlagDto.BehandlingsresultatDto.VilkårType> finnVilkårTyper(Behandlingsresultat behandlingsresultat) {
        return behandlingsresultat.getVilkårResultat().getVilkårTyper().stream().map(BrevGrunnlagTjeneste::mapVilkårType).toList();
    }

    private static BrevGrunnlagDto.BehandlingsresultatDto.VilkårType mapVilkårType(VilkårType vilkårType) {
        return switch (vilkårType) {
            case FØDSELSVILKÅRET_MOR -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.FØDSELSVILKÅRET_MOR;
            case FØDSELSVILKÅRET_FAR_MEDMOR -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR;
            case ADOPSJONSVILKARET_FORELDREPENGER -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.ADOPSJONSVILKARET_FORELDREPENGER;
            case MEDLEMSKAPSVILKÅRET -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.MEDLEMSKAPSVILKÅRET;
            case MEDLEMSKAPSVILKÅRET_FORUTGÅENDE -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE;
            case MEDLEMSKAPSVILKÅRET_LØPENDE -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE;
            case SØKNADSFRISTVILKÅRET -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.SØKNADSFRISTVILKÅRET;
            case ADOPSJONSVILKÅRET_ENGANGSSTØNAD -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD;
            case OMSORGSVILKÅRET -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.OMSORGSVILKÅRET;
            case FORELDREANSVARSVILKÅRET_2_LEDD -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD;
            case FORELDREANSVARSVILKÅRET_4_LEDD -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD;
            case SØKERSOPPLYSNINGSPLIKT -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.SØKERSOPPLYSNINGSPLIKT;
            case OPPTJENINGSPERIODEVILKÅR -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.OPPTJENINGSPERIODEVILKÅR;
            case OPPTJENINGSVILKÅRET -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.OPPTJENINGSVILKÅRET;
            case BEREGNINGSGRUNNLAGVILKÅR -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
            case SVANGERSKAPSPENGERVILKÅR -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.SVANGERSKAPSPENGERVILKÅR;
            case UDEFINERT -> BrevGrunnlagDto.BehandlingsresultatDto.VilkårType.UDEFINERT;
        };
    }

    private List<BrevGrunnlagDto.BehandlingsresultatDto.KonsekvensForYtelsen> finnKonsekvenserForYtelsen(Behandlingsresultat br) {
        return br.getKonsekvenserForYtelsen().stream().map(BrevGrunnlagTjeneste::mapKonsekvensForYtelsen).toList();
    }

    private static BrevGrunnlagDto.BehandlingsresultatDto.KonsekvensForYtelsen mapKonsekvensForYtelsen(KonsekvensForYtelsen konsekvensForYtelsen) {
        return switch (konsekvensForYtelsen) {
            case FORELDREPENGER_OPPHØRER -> BrevGrunnlagDto.BehandlingsresultatDto.KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER;
            case ENDRING_I_BEREGNING -> BrevGrunnlagDto.BehandlingsresultatDto.KonsekvensForYtelsen.ENDRING_I_BEREGNING;
            case ENDRING_I_UTTAK -> BrevGrunnlagDto.BehandlingsresultatDto.KonsekvensForYtelsen.ENDRING_I_UTTAK;
            case ENDRING_I_FORDELING_AV_YTELSEN -> BrevGrunnlagDto.BehandlingsresultatDto.KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN;
            case INGEN_ENDRING -> BrevGrunnlagDto.BehandlingsresultatDto.KonsekvensForYtelsen.INGEN_ENDRING;
            case UDEFINERT -> BrevGrunnlagDto.BehandlingsresultatDto.KonsekvensForYtelsen.UDEFINERT;
        };
    }

    private Optional<LocalDate> finnOpphørsdato(Behandling behandling) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            return Optional.empty();
        }
        return uttakTjeneste.hentHvisEksisterer(behandling.getId()).flatMap(Uttak::opphørsdato);
    }

    private Optional<BrevGrunnlagDto.BehandlingsresultatDto.SkjæringstidspunktDto> finnSkjæringstidspunktForBehandling(Behandling behandling,
                                                                                                                       Behandlingsresultat behandlingsresultat) {
        if (!behandling.erYtelseBehandling() || behandlingsresultat.isBehandlingHenlagt()) {
            return Optional.empty();
        }
        try {
            var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
            return Optional.of(
                new BrevGrunnlagDto.BehandlingsresultatDto.SkjæringstidspunktDto(stp.getUtledetSkjæringstidspunkt(), stp.utenMinsterett()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<BrevGrunnlagDto.BehandlingsresultatDto.Fritekst> finnAvslagsårsakFritekst(Behandling behandling) {
        return behandlingDokumentRepository.hentHvisEksisterer(behandling.getId())
            .filter(BehandlingDokumentEntitet::harFritekst)
            .map(behandlingDokument -> {
                var overskrift = behandlingDokument.getOverstyrtBrevOverskrift();
                var fritekst = Optional.ofNullable(behandlingDokument.getOverstyrtBrevFritekstHtml())
                    .orElse(behandlingDokument.getOverstyrtBrevFritekst());
                var avslagsarsakFritekst = behandlingDokument.getVedtakFritekst();
                return new BrevGrunnlagDto.BehandlingsresultatDto.Fritekst(overskrift, fritekst, avslagsarsakFritekst);
            });
    }

    private BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak mapAvslagsårsak(Avslagsårsak avslagsårsak) {
        return switch (avslagsårsak) {
            case SØKT_FOR_TIDLIG -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKT_FOR_TIDLIG;
            case SØKER_ER_MEDMOR -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_ER_MEDMOR;
            case SØKER_ER_FAR -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_ER_FAR;
            case BARN_OVER_15_ÅR -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.BARN_OVER_15_ÅR;
            case EKTEFELLES_SAMBOERS_BARN -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.EKTEFELLES_SAMBOERS_BARN;
            case MANN_ADOPTERER_IKKE_ALENE -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.MANN_ADOPTERER_IKKE_ALENE;
            case SØKT_FOR_SENT -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKT_FOR_SENT;
            case SØKER_ER_IKKE_BARNETS_FAR_O -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_O;
            case MOR_IKKE_DØD -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.MOR_IKKE_DØD;
            case MOR_IKKE_DØD_VED_FØDSEL_OMSORG -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.MOR_IKKE_DØD_VED_FØDSEL_OMSORG;
            case ENGANGSSTØNAD_ALLEREDE_UTBETALT_TIL_MOR ->
                BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.ENGANGSSTØNAD_ALLEREDE_UTBETALT_TIL_MOR;
            case FAR_HAR_IKKE_OMSORG_FOR_BARNET -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.FAR_HAR_IKKE_OMSORG_FOR_BARNET;
            case BARN_IKKE_UNDER_15_ÅR -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.BARN_IKKE_UNDER_15_ÅR;
            case SØKER_HAR_IKKE_FORELDREANSVAR -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_HAR_IKKE_FORELDREANSVAR;
            case SØKER_HAR_HATT_VANLIG_SAMVÆR_MED_BARNET ->
                BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_HAR_HATT_VANLIG_SAMVÆR_MED_BARNET;
            case SØKER_ER_IKKE_BARNETS_FAR_F -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_ER_IKKE_BARNETS_FAR_F;
            case OMSORGSOVERTAKELSE_ETTER_56_UKER -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.OMSORGSOVERTAKELSE_ETTER_56_UKER;
            case IKKE_FORELDREANSVAR_ALENE_ETTER_BARNELOVA ->
                BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.IKKE_FORELDREANSVAR_ALENE_ETTER_BARNELOVA;
            case MANGLENDE_DOKUMENTASJON -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.MANGLENDE_DOKUMENTASJON;
            case SØKER_ER_IKKE_MEDLEM -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_ER_IKKE_MEDLEM;
            case SØKER_ER_UTVANDRET -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_ER_UTVANDRET;
            case SØKER_HAR_IKKE_LOVLIG_OPPHOLD -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_HAR_IKKE_LOVLIG_OPPHOLD;
            case SØKER_HAR_IKKE_OPPHOLDSRETT -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_HAR_IKKE_OPPHOLDSRETT;
            case SØKER_ER_IKKE_BOSATT -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_ER_IKKE_BOSATT;
            case FØDSELSDATO_IKKE_OPPGITT_ELLER_REGISTRERT ->
                BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.FØDSELSDATO_IKKE_OPPGITT_ELLER_REGISTRERT;
            case INGEN_BARN_DOKUMENTERT_PÅ_FAR_MEDMOR -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.INGEN_BARN_DOKUMENTERT_PÅ_FAR_MEDMOR;
            case MOR_FYLLER_IKKE_VILKÅRET_FOR_SYKDOM -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.MOR_FYLLER_IKKE_VILKÅRET_FOR_SYKDOM;
            case BRUKER_ER_IKKE_REGISTRERT_SOM_FAR_MEDMOR_TIL_BARNET ->
                BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.BRUKER_ER_IKKE_REGISTRERT_SOM_FAR_MEDMOR_TIL_BARNET;
            case ENGANGSTØNAD_ER_ALLEREDE_UTBETAL_TIL_MOR ->
                BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.ENGANGSTØNAD_ER_ALLEREDE_UTBETAL_TIL_MOR;
            case FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_MOR ->
                BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_MOR;
            case ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR ->
                BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR;
            case FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR ->
                BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR;
            case IKKE_TILSTREKKELIG_OPPTJENING -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING;
            case FOR_LAVT_BEREGNINGSGRUNNLAG -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG;
            case STEBARNSADOPSJON_IKKE_FLERE_DAGER_IGJEN ->
                BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.STEBARNSADOPSJON_IKKE_FLERE_DAGER_IGJEN;
            case SØKER_INNFLYTTET_FOR_SENT -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_INNFLYTTET_FOR_SENT;
            case SØKER_IKKE_GRAVID_KVINNE -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_IKKE_GRAVID_KVINNE;
            case SØKER_ER_IKKE_I_ARBEID -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_ER_IKKE_I_ARBEID;
            case SØKER_HAR_MOTTATT_SYKEPENGER -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SØKER_HAR_MOTTATT_SYKEPENGER;
            case ARBEIDSTAKER_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER ->
                BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.ARBEIDSTAKER_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER;
            case ARBEIDSTAKER_KAN_OMPLASSERES -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.ARBEIDSTAKER_KAN_OMPLASSERES;
            case SN_FL_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER ->
                BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SN_FL_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER;
            case SN_FL_HAR_MULIGHET_TIL_Å_TILRETTELEGGE_SITT_VIRKE ->
                BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.SN_FL_HAR_MULIGHET_TIL_Å_TILRETTELEGGE_SITT_VIRKE;
            case INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN ->
                BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN;
            case UDEFINERT -> BrevGrunnlagDto.BehandlingsresultatDto.Avslagsårsak.UDEFINERT;
        };
    }

    private static BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType mapBehandlingResultatType(BehandlingResultatType behandlingResultatType) {
        return switch (behandlingResultatType) {
            case IKKE_FASTSATT -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.IKKE_FASTSATT;
            case INNVILGET -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.INNVILGET;
            case AVSLÅTT -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.AVSLÅTT;
            case OPPHØR -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.OPPHØR;
            case HENLAGT_SØKNAD_TRUKKET -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;
            case HENLAGT_FEILOPPRETTET -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.HENLAGT_FEILOPPRETTET;
            case HENLAGT_BRUKER_DØD -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.HENLAGT_BRUKER_DØD;
            case MERGET_OG_HENLAGT -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.MERGET_OG_HENLAGT;
            case HENLAGT_SØKNAD_MANGLER -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.HENLAGT_SØKNAD_MANGLER;
            case FORELDREPENGER_ENDRET -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.FORELDREPENGER_ENDRET;
            case FORELDREPENGER_SENERE -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.FORELDREPENGER_SENERE;
            case INGEN_ENDRING -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.INGEN_ENDRING;
            case MANGLER_BEREGNINGSREGLER -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.MANGLER_BEREGNINGSREGLER;
            case KLAGE_AVVIST -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.KLAGE_AVVIST;
            case KLAGE_MEDHOLD -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.KLAGE_MEDHOLD;
            case KLAGE_DELVIS_MEDHOLD -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.KLAGE_DELVIS_MEDHOLD;
            case KLAGE_OMGJORT_UGUNST -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.KLAGE_OMGJORT_UGUNST;
            case KLAGE_YTELSESVEDTAK_OPPHEVET -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.KLAGE_YTELSESVEDTAK_OPPHEVET;
            case KLAGE_YTELSESVEDTAK_STADFESTET -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET;
            case KLAGE_TILBAKEKREVING_VEDTAK_STADFESTET ->
                BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.KLAGE_TILBAKEKREVING_VEDTAK_STADFESTET;
            case HENLAGT_KLAGE_TRUKKET -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.HENLAGT_KLAGE_TRUKKET;
            case HJEMSENDE_UTEN_OPPHEVE -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.HJEMSENDE_UTEN_OPPHEVE;
            case ANKE_AVVIST -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.ANKE_AVVIST;
            case ANKE_MEDHOLD -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.ANKE_MEDHOLD;
            case ANKE_DELVIS_MEDHOLD -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.ANKE_DELVIS_MEDHOLD;
            case ANKE_OMGJORT_UGUNST -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.ANKE_OMGJORT_UGUNST;
            case ANKE_OPPHEVE_OG_HJEMSENDE -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.ANKE_OPPHEVE_OG_HJEMSENDE;
            case ANKE_HJEMSENDE_UTEN_OPPHEV -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.ANKE_HJEMSENDE_UTEN_OPPHEV;
            case ANKE_YTELSESVEDTAK_STADFESTET -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.ANKE_YTELSESVEDTAK_STADFESTET;
            case HENLAGT_ANKE_TRUKKET -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.HENLAGT_ANKE_TRUKKET;
            case INNSYN_INNVILGET -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.INNSYN_INNVILGET;
            case INNSYN_DELVIS_INNVILGET -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.INNSYN_DELVIS_INNVILGET;
            case INNSYN_AVVIST -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.INNSYN_AVVIST;
            case HENLAGT_INNSYN_TRUKKET -> BrevGrunnlagDto.BehandlingsresultatDto.BehandlingResultatType.HENLAGT_INNSYN_TRUKKET;
        };
    }

    private BrevGrunnlagDto.RettigheterDto utledRettigheter(Behandling behandling) {
        var opprinnelig = opprinneligRettighetstype(behandling);
        var gjeldende = gjeldendeRettighetstype(behandling);

        var eøsUttak = utledEøsUttak(behandling);
        return new BrevGrunnlagDto.RettigheterDto(opprinnelig, gjeldende, eøsUttak.orElse(null));
    }

    private Optional<BrevGrunnlagDto.RettigheterDto.EøsUttakDto> utledEøsUttak(Behandling behandling) {
        return eøsUttakRepository.hentGrunnlag(behandling.getId()).flatMap(eøsUttak -> {
            var fom = eøsUttak.getFom();
            if (fom.isEmpty()) {
                return Optional.empty();
            }
            var tom = eøsUttak.getTom().orElseThrow();
            var forbruktFellesperiode = eøsUttak.getPerioder()
                .stream()
                .filter(p -> p.getTrekkonto() == UttakPeriodeType.FELLESPERIODE)
                .map(EøsUttaksperiodeEntitet::getTrekkdager)
                .reduce(Trekkdager::add)
                .orElse(Trekkdager.ZERO);
            var maksdagerFellesperiode = utregnetStønadskontoTjeneste.gjeldendeKontoutregning(BehandlingReferanse.fra(behandling))
                .get(StønadskontoType.FELLESPERIODE);
            var fellesperiodeINorge = new Trekkdager(maksdagerFellesperiode).subtract(forbruktFellesperiode)
                .decimalValue()
                .setScale(0, RoundingMode.UP)
                .intValue();
            var forbruktFellesperiodeInt = forbruktFellesperiode.decimalValue().setScale(0, RoundingMode.DOWN).intValue();
            return Optional.of(
                new BrevGrunnlagDto.RettigheterDto.EøsUttakDto(fom.get(), tom, forbruktFellesperiodeInt, Math.max(fellesperiodeINorge, 0)));
        });
    }

    private Rettighetstype opprinneligRettighetstype(Behandling behandling) {
        if (behandling.erRevurdering()) {
            var originalBehandlingId = behandling.getOriginalBehandlingId().orElseThrow();
            return gjeldendeRettighetstype(behandlingRepository.hentBehandling(originalBehandlingId));
        }
        var yfa = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        return yfa.getOppgittRettighet().rettighetstype(behandling.getRelasjonsRolleType());
    }

    private Rettighetstype gjeldendeRettighetstype(Behandling behandling) {
        var stønadskontoberegning = utregnetStønadskontoTjeneste.gjeldendeKontoutregning(BehandlingReferanse.fra(behandling));
        var yfa = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        var relasjonsRolleType = behandling.getRelasjonsRolleType();
        if (stønadskontoberegning.keySet().stream().anyMatch(stønadskontoType -> stønadskontoType.equals(StønadskontoType.FORELDREPENGER))) {
            if (yfa.robustHarAleneomsorg(relasjonsRolleType)) {
                return Rettighetstype.ALENEOMSORG;
            }
            if (relasjonsRolleType.erFarEllerMedMor()) {
                if (yfa.morMottarUføretrygd(uføretrygdRepository.hentGrunnlag(behandling.getId()).orElse(null))) {
                    return Rettighetstype.BARE_FAR_RETT_MOR_UFØR;
                }
                return Rettighetstype.BARE_FAR_RETT;
            }
            return Rettighetstype.BARE_MOR_RETT;
        }
        return yfa.avklartAnnenForelderHarRettEØS() ? Rettighetstype.BEGGE_RETT_EØS : Rettighetstype.BEGGE_RETT;
    }

    private Optional<BrevGrunnlagDto.MedlemskapOpphørsÅrsak> finnMedlemskapOpphørsÅrsak(Behandling behandling) {
        return uttakTjeneste.hentHvisEksisterer(behandling.getId())
            .filter(Uttak::harAvslagPgaMedlemskap)
            .flatMap(u -> medlemTjeneste.hentAvslagsårsak(behandling.getId()))
            .map(BrevGrunnlagTjeneste::map);
    }

    private Optional<BrevGrunnlagDto.FamilieHendelseDto> finnFamilieHendelse(Long behandlingId) {
        return familieHendelseTjeneste.finnAggregat(behandlingId).map(fh -> {
            var barn = fh.getGjeldendeBarna()
                .stream()
                .map(b -> new BrevGrunnlagDto.BarnDto(b.getFødselsdato(), b.getDødsdato().orElse(null)))
                .toList();
            var termindato = fh.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato).orElse(null);
            var antallBarn = fh.getGjeldendeAntallBarn();
            return new BrevGrunnlagDto.FamilieHendelseDto(barn, termindato, antallBarn,
                fh.getGjeldendeAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato).orElse(null));
        });
    }

    private BrevGrunnlagDto.Språkkode finnSpråkkode(Behandling behandling) {
        var språkkode = søknadRepository.hentSøknadHvisEksisterer(behandling.getId())
            .map(SøknadEntitet::getSpråkkode)
            .orElseGet(() -> behandling.getFagsak().getNavBruker().getSpråkkode());
        return mapSpråkkode(språkkode);
    }

    private static BrevGrunnlagDto.MedlemskapOpphørsÅrsak map(Avslagsårsak avslagsårsak) {
        return switch (avslagsårsak) {
            case Avslagsårsak.SØKER_ER_IKKE_MEDLEM -> BrevGrunnlagDto.MedlemskapOpphørsÅrsak.SØKER_ER_IKKE_MEDLEM;
            case Avslagsårsak.SØKER_ER_UTVANDRET -> BrevGrunnlagDto.MedlemskapOpphørsÅrsak.SØKER_ER_UTVANDRET;
            case Avslagsårsak.SØKER_HAR_IKKE_LOVLIG_OPPHOLD -> BrevGrunnlagDto.MedlemskapOpphørsÅrsak.SØKER_HAR_IKKE_LOVLIG_OPPHOLD;
            case Avslagsårsak.SØKER_HAR_IKKE_OPPHOLDSRETT -> BrevGrunnlagDto.MedlemskapOpphørsÅrsak.SØKER_HAR_IKKE_OPPHOLDSRETT;
            case Avslagsårsak.SØKER_ER_IKKE_BOSATT -> BrevGrunnlagDto.MedlemskapOpphørsÅrsak.SØKER_ER_IKKE_BOSATT;
            case Avslagsårsak.SØKER_INNFLYTTET_FOR_SENT -> BrevGrunnlagDto.MedlemskapOpphørsÅrsak.SØKER_INNFLYTTET_FOR_SENT;
            default -> throw new IllegalStateException("Ny medlemskap avslag ikke støttet i brev" + avslagsårsak);
        };
    }

    private static BrevGrunnlagDto.Språkkode mapSpråkkode(Språkkode språkkode) {
        return switch (språkkode) {
            case NB -> BrevGrunnlagDto.Språkkode.BOKMÅL;
            case NN -> BrevGrunnlagDto.Språkkode.NYNORSK;
            case EN -> BrevGrunnlagDto.Språkkode.ENGELSK;
            case UDEFINERT -> throw new IllegalStateException("Unexpected value: " + språkkode);
        };
    }

    private static BrevGrunnlagDto.BehandlingType mapBehandlingType(BehandlingType type) {
        return switch (type) {
            case FØRSTEGANGSSØKNAD -> BrevGrunnlagDto.BehandlingType.FØRSTEGANGSSØKNAD;
            case KLAGE -> BrevGrunnlagDto.BehandlingType.KLAGE;
            case REVURDERING -> BrevGrunnlagDto.BehandlingType.REVURDERING;
            case ANKE -> BrevGrunnlagDto.BehandlingType.ANKE;
            case INNSYN -> BrevGrunnlagDto.BehandlingType.INNSYN;
            case TILBAKEKREVING_ORDINÆR -> BrevGrunnlagDto.BehandlingType.TILBAKEKREVING_ORDINÆR;
            case TILBAKEKREVING_REVURDERING -> BrevGrunnlagDto.BehandlingType.TILBAKEKREVING_REVURDERING;
            case UDEFINERT -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }
}
