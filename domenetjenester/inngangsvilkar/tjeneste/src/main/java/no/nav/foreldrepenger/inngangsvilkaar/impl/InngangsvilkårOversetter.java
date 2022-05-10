package no.nav.foreldrepenger.inngangsvilkaar.impl;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelKjønn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.AdopsjonsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.BekreftetAdopsjon;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.BekreftetAdopsjonBarn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel.FødselsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.RegelPersonStatusType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.søknadsfrist.SøknadsfristvilkårGrunnlag;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class InngangsvilkårOversetter {

    private static final Map<NavBrukerKjønn, RegelKjønn> MAP_KJØNN = Map.of(
        NavBrukerKjønn.KVINNE, RegelKjønn.KVINNE,
        NavBrukerKjønn.MANN, RegelKjønn.MANN
    );

    private static final Map<RelasjonsRolleType, RegelSøkerRolle> MAP_ROLLE_TYPE = Map.of(
        RelasjonsRolleType.MORA, RegelSøkerRolle.MORA,
        RelasjonsRolleType.MEDMOR, RegelSøkerRolle.MEDMOR,
        RelasjonsRolleType.FARA, RegelSøkerRolle.FARA
    );

    private static final Map<PersonstatusType, RegelPersonStatusType> MAP_PERSONSTATUS_TYPE = Map.of(
        PersonstatusType.BOSA, RegelPersonStatusType.BOSA,
        PersonstatusType.ADNR, RegelPersonStatusType.BOSA,
        PersonstatusType.UTVA, RegelPersonStatusType.UTVA,
        PersonstatusType.DØD, RegelPersonStatusType.DØD
    );

    private MedlemskapRepository medlemskapRepository;
    private FamilieHendelseRepository familieGrunnlagRepository;
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;
    private SøknadRepository søknadRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private YtelseMaksdatoTjeneste ytelseMaksdatoTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    private Period tidligstUtstedelseFørTermin;

    InngangsvilkårOversetter() {
        // for CDI proxy
    }

    /**
     * @param tidligsteUtstedelseAvTerminBekreftelse - Periode for tidligst utstedelse av terminbekreftelse før termindato
     */
    @Inject
    public InngangsvilkårOversetter(BehandlingRepositoryProvider repositoryProvider,
                                    PersonopplysningTjeneste personopplysningTjeneste,
                                    YtelseMaksdatoTjeneste beregnMorsMaksdatoTjeneste,
                                    InntektArbeidYtelseTjeneste iayTjeneste,
                                    @KonfigVerdi(value = "terminbekreftelse.tidligst.utstedelse.før.termin", defaultVerdi = "P18W3D") Period tidligsteUtstedelseAvTerminBekreftelse) {
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.iayTjeneste = iayTjeneste;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.medlemskapPerioderTjeneste = new MedlemskapPerioderTjeneste();
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.ytelseMaksdatoTjeneste = beregnMorsMaksdatoTjeneste;
        this.tidligstUtstedelseFørTermin = tidligsteUtstedelseAvTerminBekreftelse;
    }

    public FødselsvilkårGrunnlag oversettTilRegelModellFødsel(BehandlingReferanse ref) {
        var medFarMedmorUttakRundtFødsel = !ref.getSkjæringstidspunkt().utenMinsterett();
        final var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregat(ref.behandlingId());
        var bekreftetFamilieHendelse = familieHendelseGrunnlag.getGjeldendeBekreftetVersjon();
        var gjeldendeTerminbekreftelse = familieHendelseGrunnlag.getGjeldendeTerminbekreftelse();
        var kjønn = tilSøkerKjøenn(getSøkersKjønn(ref));
        var rolle = finnSoekerRolle(ref).orElse(null);
        var bekreftetFødselsDato = bekreftetFamilieHendelse.flatMap(FamilieHendelseEntitet::getFødselsdato).orElse(null);
        var gjeldendeTermindato = gjeldendeTerminbekreftelse.map(TerminbekreftelseEntitet::getTermindato).orElse(null);
        var gjeldendeUtstedtDato = gjeldendeTerminbekreftelse.map(TerminbekreftelseEntitet::getUtstedtdato).orElse(null);
        var antallbarn = bekreftetFamilieHendelse.map(FamilieHendelseEntitet::getAntallBarn).orElse(0);
        var fristRegistreringUtløpt = FamilieHendelseTjeneste.getManglerFødselsRegistreringFristUtløpt(familieHendelseGrunnlag);
        var morForSykVedFødsel = bekreftetFamilieHendelse.map(FamilieHendelseEntitet::erMorForSykVedFødsel).orElse(false);
        var søktOmTermin = erSøktOmTermin(familieHendelseGrunnlag.getSøknadVersjon());
        var behandlingsdatoEtterTidligsteDato = erBehandlingsdatoEtterTidligsteDato(gjeldendeTermindato);
        var terminbekreftelseUtstedtEtterTidligsteDato = erTerminbekreftelseUtstedtEtterTidligsteDato(gjeldendeTermindato, gjeldendeUtstedtDato);

        var grunnlag = new FødselsvilkårGrunnlag(kjønn, rolle, LocalDate.now(),
            bekreftetFødselsDato, gjeldendeTermindato,
            antallbarn,
            fristRegistreringUtløpt,
            morForSykVedFødsel, søktOmTermin,
            behandlingsdatoEtterTidligsteDato,
            terminbekreftelseUtstedtEtterTidligsteDato, medFarMedmorUttakRundtFødsel);
        return grunnlag;
    }

    /**
     * Presisering fra fag: Fra og med man er i svangerskapsuke 22 kan man søke og få innvilget ES/FP.
     * Dette er tolket som FOMdato = termindato - 18uker - 3dager
     */
    private boolean erTerminbekreftelseUtstedtEtterTidligsteDato(LocalDate termindato, LocalDate utstedtDato) {
        if (termindato == null || utstedtDato == null) {
            return true;
        }
        var tidligstedatoMinusDag = termindato.minus(tidligstUtstedelseFørTermin).minusDays(1);
        return utstedtDato.isAfter(tidligstedatoMinusDag);
    }

    private boolean erBehandlingsdatoEtterTidligsteDato(LocalDate termindato) {
        if (termindato == null) {
            return true;
        }
        var tidligstedatoMinusDag = termindato.minus(tidligstUtstedelseFørTermin).minusDays(1);
        return LocalDate.now().isAfter(tidligstedatoMinusDag);
    }

    private boolean erSøktOmTermin(FamilieHendelseEntitet familieHendelse) {
        var type = familieHendelse.getType();
        return FamilieHendelseType.TERMIN.equals(type);
    }

    private NavBrukerKjønn getSøkersKjønn(BehandlingReferanse ref) {
        try {
            return personopplysningTjeneste.hentPersonopplysninger(ref).getSøker().getKjønn();
        } catch (Exception e) {
            return NavBrukerKjønn.UDEFINERT;
        }
    }

    private Optional<RegelSøkerRolle> finnSoekerRolle(BehandlingReferanse ref) {
        return Optional.ofNullable(finnRelasjonRolle(ref)).map(MAP_ROLLE_TYPE::get);
    }

    private RelasjonsRolleType finnRelasjonRolle(BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        final var hendelseGrunnlag = familieGrunnlagRepository.hentAggregat(behandlingId);
        if (hendelseGrunnlag.getGjeldendeBekreftetVersjon().isEmpty()) {
            // Kan ikke finne relasjonsrolle dersom fødsel ikke er bekreftet.
            return null;
        }
        final var familieHendelse = hendelseGrunnlag.getGjeldendeBekreftetVersjon().get();
        final var fødselsdato = familieHendelse.getBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst();

        if (!fødselsdato.isPresent()) {
            return null;
        }

        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);

        final var fødselIntervall = byggIntervall(fødselsdato.get(), fødselsdato.get());
        var alleBarnPåFødselsdato = personopplysninger.getAlleBarnFødtI(fødselIntervall);

        var søkerPersonopplysning = personopplysninger.getSøker();
        var søkersAktørId = søkerPersonopplysning.getAktørId();

        if (!alleBarnPåFødselsdato.isEmpty()) {
            // Forutsetter at barn som er født er tvillinger, og sjekker derfor bare første barn.
            final var personRelasjon = personopplysninger.getRelasjoner()
                .stream()
                .filter(relasjon -> relasjon.getTilAktørId().equals(søkersAktørId))
                .filter(familierelasjon -> RelasjonsRolleType.erRegistrertForeldre(familierelasjon.getRelasjonsrolle()))
                .findFirst();

            return personRelasjon.map(PersonRelasjonEntitet::getRelasjonsrolle).orElse(ref.relasjonRolle());
        }
        // Har ingenting annet å gå på så benytter det søker oppgir.
        return ref.relasjonRolle();
    }

    private SimpleLocalDateInterval byggIntervall(LocalDate fomDato, LocalDate tomDato) {
        return SimpleLocalDateInterval.fraOgMedTomNotNull(fomDato, tomDato);
    }

    public SøknadsfristvilkårGrunnlag oversettTilRegelModellSøknad(BehandlingReferanse ref) {
        final var søknad = søknadRepository.hentSøknad(ref.behandlingId());
        var skjæringsdato = ref.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        return new SøknadsfristvilkårGrunnlag(
            søknad.getElektroniskRegistrert(),
            skjæringsdato,
            søknad.getMottattDato());
    }

    public AdopsjonsvilkårGrunnlag oversettTilRegelModellAdopsjon(BehandlingReferanse ref) {
        var bekreftetAdopsjon = byggBekreftetAdopsjon(ref);
        var adopsjonBarn = bekreftetAdopsjon.adopsjonBarn();
        return new AdopsjonsvilkårGrunnlag(
            adopsjonBarn,
            bekreftetAdopsjon.ektefellesBarn(),
            tilSøkerKjøenn(getSøkersKjønn(ref)),
            bekreftetAdopsjon.adoptererAlene(),
            bekreftetAdopsjon.omsorgsovertakelseDato(),
            erStønadperiodeBruktOpp(ref));
    }

    private boolean erStønadperiodeBruktOpp(BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        final var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregat(behandlingId);
        var versjon = familieHendelseGrunnlag.getGjeldendeBekreftetVersjon();
        var familieHendelse = versjon.orElseGet(familieHendelseGrunnlag::getSøknadVersjon);

        // TODO PK-48734 - er omsorgsovertakelseDato riktig dato?
        if (familieHendelse.getAdopsjon().isPresent()) {
            var omsorgsovertakelseDato = familieHendelse.getAdopsjon().get().getOmsorgsovertakelseDato();
            var maksdatoForeldrepenger = ytelseMaksdatoTjeneste.beregnMaksdatoForeldrepenger(ref);

            if (maksdatoForeldrepenger.isEmpty() || omsorgsovertakelseDato.isBefore(maksdatoForeldrepenger.get())) {
                return false; // stønadsperioden er ikke brukt opp av annen forelder
            }
        }
        return true;
    }

    public MedlemskapsvilkårGrunnlag oversettTilRegelModellMedlemskap(BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);
        var iayOpt = iayTjeneste.finnGrunnlag(behandlingId);

        var medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);

        var vurdertMedlemskap = medlemskap.flatMap(MedlemskapAggregat::getVurdertMedlemskap);

        // // FP VK 2.13
        var vurdertErMedlem = brukerErMedlemEllerIkkeRelevantPeriode(medlemskap, personopplysninger, ref.getSkjæringstidspunkt());
        // FP VK 2.2 Er bruker avklart som pliktig eller frivillig medlem?
        var avklartPliktigEllerFrivillig = erAvklartSomPliktigEllerFrivillingMedlem(medlemskap, ref.getSkjæringstidspunkt());
        // defaulter uavklarte fakta til true
        var vurdertBosatt = vurdertMedlemskap.map(VurdertMedlemskap::getBosattVurdering).orElse(true);
        var vurdertLovligOpphold = vurdertMedlemskap.map(VurdertMedlemskap::getLovligOppholdVurdering).orElse(true);
        var vurdertOppholdsrett = vurdertMedlemskap.map(VurdertMedlemskap::getOppholdsrettVurdering).orElse(true);

        var harOppholdstillatelse = personopplysningTjeneste.harOppholdstillatelseForPeriode(ref.behandlingId(), ref.getUtledetMedlemsintervall());
        var harArbeidInntekt = FinnOmSøkerHarArbeidsforholdOgInntekt.finn(iayOpt, ref.getUtledetSkjæringstidspunkt(), ref.aktørId());

        var grunnlag = new MedlemskapsvilkårGrunnlag(
            tilPersonStatusType(personopplysninger), // FP VK 2.1
            brukerNorskNordisk(personopplysninger), // FP VK 2.11
            brukerBorgerAvEOS(vurdertMedlemskap, personopplysninger), // FP VIK 2.12
            harOppholdstillatelse,
            harArbeidInntekt,
            vurdertErMedlem,
            avklartPliktigEllerFrivillig,
            vurdertBosatt,
            vurdertLovligOpphold,
            vurdertOppholdsrett);

        return grunnlag;
    }

    /**
     * True dersom saksbehandler har vurdert til å være medlem i relevant periode
     */
    private boolean erAvklartSomPliktigEllerFrivillingMedlem(Optional<MedlemskapAggregat> medlemskap, Skjæringstidspunkt skjæringstidspunkter) {
        if (medlemskap.isPresent()) {
            var vurdertMedlemskapOpt = medlemskap.get().getVurdertMedlemskap();
            if (vurdertMedlemskapOpt.isPresent()) {
                var vurdertMedlemskap = vurdertMedlemskapOpt.get();
                if (vurdertMedlemskap.getMedlemsperiodeManuellVurdering() != null &&
                    MedlemskapManuellVurderingType.MEDLEM.equals(vurdertMedlemskap.getMedlemsperiodeManuellVurdering())) {
                    return true;
                }
                if (vurdertMedlemskap.getMedlemsperiodeManuellVurdering() != null &&
                    MedlemskapManuellVurderingType.IKKE_RELEVANT.equals(vurdertMedlemskap.getMedlemsperiodeManuellVurdering())) {
                    return false;
                }
            }
            return medlemskapPerioderTjeneste.brukerMaskineltAvklartSomFrivilligEllerPliktigMedlem(
                medlemskap.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder).orElse(Collections.emptySet()),
                skjæringstidspunkter.getUtledetSkjæringstidspunkt());
        }
        return false;
    }

    /**
     * True dersom saksbehandler har vurdert til ikke å være medlem i relevant periode
     */
    private static boolean erAvklartSomIkkeMedlem(Optional<VurdertMedlemskap> medlemskap) {
        return medlemskap.isPresent() && medlemskap.get().getMedlemsperiodeManuellVurdering() != null
            && MedlemskapManuellVurderingType.UNNTAK.equals(medlemskap.get().getMedlemsperiodeManuellVurdering());
    }

    private boolean brukerErMedlemEllerIkkeRelevantPeriode(Optional<MedlemskapAggregat> medlemskap, PersonopplysningerAggregat søker,
                                                           Skjæringstidspunkt skjæringstidspunkter) {
        var vurdertMedlemskap = medlemskap.flatMap(MedlemskapAggregat::getVurdertMedlemskap);
        if (vurdertMedlemskap.isPresent()
            && MedlemskapManuellVurderingType.IKKE_RELEVANT.equals(vurdertMedlemskap.get().getMedlemsperiodeManuellVurdering())) {
            return true;
        }

        Set<MedlemskapPerioderEntitet> medlemskapPerioder = medlemskap.isPresent() ? medlemskap.get().getRegistrertMedlemskapPerioder()
            : Collections.emptySet();
        var erAvklartMaskineltSomIkkeMedlem = medlemskapPerioderTjeneste.brukerMaskineltAvklartSomIkkeMedlem(søker,
            medlemskapPerioder, skjæringstidspunkter.getUtledetSkjæringstidspunkt());
        var erAvklartManueltSomIkkeMedlem = erAvklartSomIkkeMedlem(vurdertMedlemskap);

        return !(erAvklartMaskineltSomIkkeMedlem || erAvklartManueltSomIkkeMedlem);
    }

    private static boolean brukerBorgerAvEOS(Optional<VurdertMedlemskap> medlemskap, PersonopplysningerAggregat aggregat) {
        // Tar det første for det er det som er prioritert høyest rangert på region
        var eosBorger = aggregat.harStatsborgerskapRegionVedSkjæringstidspunkt(aggregat.getSøker().getAktørId(), Region.EOS);
        return medlemskap
            .map(VurdertMedlemskap::getErEøsBorger)
            .orElse(eosBorger);
    }

    private static boolean brukerNorskNordisk(PersonopplysningerAggregat aggregat) {
        return aggregat.harStatsborgerskapRegionVedSkjæringstidspunkt(aggregat.getSøker().getAktørId(), Region.NORDEN);
    }

    private static RegelPersonStatusType tilPersonStatusType(PersonopplysningerAggregat personopplysninger) {
        // Bruker overstyrt personstatus hvis det finnes
        return Optional.ofNullable(personopplysninger.getPersonstatusFor(personopplysninger.getSøker().getAktørId()))
            .map(PersonstatusEntitet::getPersonstatus)
            .map(MAP_PERSONSTATUS_TYPE::get)
            .orElse(null);
    }

    private BekreftetAdopsjon byggBekreftetAdopsjon(BehandlingReferanse ref) {
        var behandlingId = ref.behandlingId();
        final var bekreftetVersjon = familieGrunnlagRepository.hentAggregat(behandlingId).getGjeldendeBekreftetVersjon();
        final var adopsjon = bekreftetVersjon.flatMap(FamilieHendelseEntitet::getAdopsjon)
            .orElseThrow(() -> new TekniskException("FP-384255",
                String.format("Ikke mulig å oversette adopsjonsgrunnlag til regelmotor for behandlingId %s", behandlingId)));

        var bekreftetAdopsjonBarn = bekreftetVersjon.map(FamilieHendelseEntitet::getBarna).orElse(List.of()).stream()
            .map(barn -> new BekreftetAdopsjonBarn(barn.getFødselsdato()))
            .collect(toList());
        var bekreftetAdopsjon = new BekreftetAdopsjon(adopsjon.getOmsorgsovertakelseDato(), bekreftetAdopsjonBarn,
            getBooleanOrDefaultFalse(adopsjon.getErEktefellesBarn()),
            getBooleanOrDefaultFalse(adopsjon.getAdoptererAlene()));
        return bekreftetAdopsjon;
    }

    private static boolean getBooleanOrDefaultFalse(Boolean bool) {
        if (bool == null) {
            return false;
        }
        return bool;
    }

    private static RegelKjønn tilSøkerKjøenn(NavBrukerKjønn søkerKjønn) {
        return Optional.ofNullable(MAP_KJØNN.get(søkerKjønn))
            .orElseThrow(() -> new NullPointerException("Fant ingen kjønn for " + søkerKjønn.getKode()));
    }

    public static VilkårData tilVilkårData(VilkårType vilkårType, Evaluation evaluation, VilkårGrunnlag grunnlag) {
        return VilkårUtfallOversetter.oversett(vilkårType, evaluation, grunnlag, null);
    }

    public static VilkårData tilVilkårData(VilkårType vilkårType, Evaluation evaluation, VilkårGrunnlag grunnlag, Object ekstraData) {
        return VilkårUtfallOversetter.oversett(vilkårType, evaluation, grunnlag, ekstraData);
    }
}
