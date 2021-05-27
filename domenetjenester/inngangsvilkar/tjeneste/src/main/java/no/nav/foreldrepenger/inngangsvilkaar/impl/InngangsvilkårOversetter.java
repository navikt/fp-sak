package no.nav.foreldrepenger.inngangsvilkaar.impl;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
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
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.PersonStatusType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.søknadsfrist.SøknadsfristvilkårGrunnlag;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class InngangsvilkårOversetter {

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
        final var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregat(ref.getId());
        var bekreftetFamilieHendelse = familieHendelseGrunnlag.getGjeldendeBekreftetVersjon();
        var gjeldendeTerminbekreftelse = familieHendelseGrunnlag.getGjeldendeTerminbekreftelse();
        var kjønn = tilSøkerKjøenn(getSøkersKjønn(ref));
        var rolle = finnSoekerRolle(ref);
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
            bekreftetFødselsDato, gjeldendeTermindato, gjeldendeUtstedtDato,
            antallbarn,
            fristRegistreringUtløpt,
            morForSykVedFødsel, søktOmTermin,
            behandlingsdatoEtterTidligsteDato,
            terminbekreftelseUtstedtEtterTidligsteDato);
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
        return personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref)
            .map(PersonopplysningerAggregat::getSøker)
            .map(PersonopplysningEntitet::getKjønn).orElse(NavBrukerKjønn.UDEFINERT);
    }

    private RegelSøkerRolle finnSoekerRolle(BehandlingReferanse ref) {
        var relasjonsRolleType = finnRelasjonRolle(ref);
        if (Objects.equals(RelasjonsRolleType.MORA, relasjonsRolleType)) {
            return RegelSøkerRolle.MORA;
        }
        if (Objects.equals(RelasjonsRolleType.FARA, relasjonsRolleType)) {
            return RegelSøkerRolle.FARA;
        }
        if (Objects.equals(RelasjonsRolleType.MEDMOR, relasjonsRolleType)) {
            return RegelSøkerRolle.MEDMOR;
        }
        return null;
    }

    private RelasjonsRolleType finnRelasjonRolle(BehandlingReferanse ref) {
        var behandlingId = ref.getBehandlingId();
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

            return personRelasjon.map(PersonRelasjonEntitet::getRelasjonsrolle).orElse(ref.getRelasjonsRolleType());
        }
        // Har ingenting annet å gå på så benytter det søker oppgir.
        return ref.getRelasjonsRolleType();
    }

    private SimpleLocalDateInterval byggIntervall(LocalDate fomDato, LocalDate tomDato) {
        return SimpleLocalDateInterval.fraOgMedTomNotNull(fomDato, tomDato);
    }

    public SøknadsfristvilkårGrunnlag oversettTilRegelModellSøknad(BehandlingReferanse ref) {
        final var søknad = søknadRepository.hentSøknad(ref.getBehandlingId());
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
        var behandlingId = ref.getBehandlingId();
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
        var behandlingId = ref.getBehandlingId();
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

        var harOppholdstillatelse = personopplysningTjeneste.harOppholdstillatelseForPeriode(ref.getBehandlingId(), ref.getUtledetMedlemsintervall());
        var harArbeidInntekt = FinnOmSøkerHarArbeidsforholdOgInntekt.finn(iayOpt, ref.getUtledetSkjæringstidspunkt(), ref.getAktørId());

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
    private boolean erAvklartSomIkkeMedlem(Optional<VurdertMedlemskap> medlemskap) {
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

    private boolean brukerBorgerAvEOS(Optional<VurdertMedlemskap> medlemskap, PersonopplysningerAggregat aggregat) {
        // Tar det første for det er det som er prioritert høyest rangert på region
        var eosBorger = aggregat.harStatsborgerskapRegion(aggregat.getSøker().getAktørId(), Region.EOS);
        return medlemskap
            .map(VurdertMedlemskap::getErEøsBorger)
            .orElse(eosBorger);
    }

    private boolean brukerNorskNordisk(PersonopplysningerAggregat aggregat) {
        return aggregat.harStatsborgerskapRegion(aggregat.getSøker().getAktørId(), Region.NORDEN);
    }

    private PersonStatusType tilPersonStatusType(PersonopplysningerAggregat personopplysninger) {
        // Bruker overstyrt personstatus hvis det finnes
        var type = Optional.ofNullable(personopplysninger.getPersonstatusFor(personopplysninger.getSøker().getAktørId()))
            .map(PersonstatusEntitet::getPersonstatus).orElse(null);

        if (PersonstatusType.BOSA.equals(type) || PersonstatusType.ADNR.equals(type)) {
            return PersonStatusType.BOSA;
        }
        if (PersonstatusType.UTVA.equals(type)) {
            return PersonStatusType.UTVA;
        }
        if (PersonstatusType.erDød(type)) {
            return PersonStatusType.DØD;
        }
        return null;
    }

    private BekreftetAdopsjon byggBekreftetAdopsjon(BehandlingReferanse ref) {
        var behandlingId = ref.getBehandlingId();
        final var bekreftetVersjon = familieGrunnlagRepository.hentAggregat(behandlingId).getGjeldendeBekreftetVersjon();
        final var adopsjon = bekreftetVersjon.flatMap(FamilieHendelseEntitet::getAdopsjon)
            .orElseThrow(() -> new TekniskException("FP-384255",
                String.format("Ikke mulig å oversette adopsjonsgrunnlag til regelmotor for behandlingId %s", behandlingId)));

        var bekreftetAdopsjonBarn = bekreftetVersjon.map(FamilieHendelseEntitet::getBarna).orElse(List.of()).stream()
            .map(barn -> new BekreftetAdopsjonBarn(barn.getFødselsdato()))
            .collect(toList());
        var bekreftetAdopsjon = new BekreftetAdopsjon(adopsjon.getOmsorgsovertakelseDato(), bekreftetAdopsjonBarn,
            adopsjon.getErEktefellesBarn(), adopsjon.getAdoptererAlene());
        return bekreftetAdopsjon;
    }

    private RegelKjønn tilSøkerKjøenn(NavBrukerKjønn søkerKjønn) {
        var kjoenn = RegelKjønn.hentKjønn(søkerKjønn.getKode());
        Objects.requireNonNull(kjoenn, "Fant ingen kjonn for: " + søkerKjønn.getKode());
        return kjoenn;
    }

    public VilkårData tilVilkårData(VilkårType vilkårType, Evaluation evaluation, VilkårGrunnlag grunnlag) {
        return new VilkårUtfallOversetter().oversett(vilkårType, evaluation, grunnlag);
    }
}
