package no.nav.foreldrepenger.datavarehus.xml.fp;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.vedtak.felles.xml.vedtak.uttak.fp.v2.FordelingPeriode;
import no.nav.vedtak.felles.xml.vedtak.uttak.fp.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.uttak.fp.v2.Stoenadskonto;
import no.nav.vedtak.felles.xml.vedtak.uttak.fp.v2.UttakForeldrepenger;
import no.nav.vedtak.felles.xml.vedtak.uttak.fp.v2.UttaksresultatPeriode;
import no.nav.vedtak.felles.xml.vedtak.uttak.fp.v2.UttaksresultatPeriodeAktivitet;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;
import no.nav.vedtak.felles.xml.vedtak.v2.Uttak;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class UttakXmlTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(UttakXmlTjeneste.class);

    private ObjectFactory uttakObjectFactory;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    public UttakXmlTjeneste() {
        //For CDI
    }

    @Inject
    public UttakXmlTjeneste(BehandlingRepositoryProvider repositoryProvider, ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.uttakObjectFactory = new ObjectFactory();
        this.uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.uttakTjeneste = uttakTjeneste;
    }

    public void setUttak(Beregningsresultat beregningsresultat, Behandling behandling) {
        forsøkV2RettighetType(behandling);

        var uttakForeldrepenger = uttakObjectFactory.createUttakForeldrepenger();

        uttaksperiodegrenseRepository.hentHvisEksisterer(behandling.getId())
            .map(Uttaksperiodegrense::getMottattDato)
            .map(Søknadsfrister::tidligsteDatoDagytelse)
            .flatMap(VedtakXmlUtil::lagDateOpplysning)
            .ifPresent(uttakForeldrepenger::setFoersteLovligeUttaksdag);

        setStoenadskontoer(uttakForeldrepenger, behandling);
        setUttaksresultatPerioder(uttakForeldrepenger, behandling);
        setFordelingPerioder(uttakForeldrepenger, behandling);

        var uttak = new Uttak();
        uttak.getAny().add(uttakObjectFactory.createUttak(uttakForeldrepenger));
        beregningsresultat.setUttak(uttak);
    }

    private void forsøkV2RettighetType(Behandling behandling) {
        try {
            utledV2RettighetType(behandling);
        } catch (Exception e) {
            LOG.info("V2 saksmodell feilet for behandling {}", behandling, e);
        }
    }

    private void utledV2RettighetType(Behandling behandling) {
        uttakTjeneste.hentUttakHvisEksisterer(behandling.getId()).ifPresent(u -> utledV2RettighetType(behandling, u));
    }

    private void utledV2RettighetType(Behandling behandling, ForeldrepengerUttak uttak) {
        var rettighetTimeline = utledRettighetTimeline(behandling, uttak);

        var behandlingForLogging = new BehRef(behandling.getFagsak().getSaksnummer(), behandling.getId(), behandling.getUuid());

        var ulikeRettighetTyper = rettighetTimeline.stream().map(LocalDateSegment::getValue).distinct().count();
        if (ulikeRettighetTyper == 0) {
            LOG.info("V2 saksmodell utledet rettigheter empty {}", behandlingForLogging);
        } else if (ulikeRettighetTyper > 1) {
            LOG.info("V2 saksmodell utledet rettigheter endret {} {}", behandlingForLogging, rettighetTimeline.toSegments());
        } else {
            LOG.info("V2 saksmodell utledet rettigheter uendret {} {}", behandlingForLogging, rettighetTimeline.toSegments());
        }
    }

    private LocalDateTimeline<RettighetType> utledRettighetTimeline(Behandling behandling, ForeldrepengerUttak uttak) {
        var yfa = ytelsesFordelingRepository.hentAggregat(behandling.getId());

        var konto = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak())
            .map(fr -> fr.getGjeldendeStønadskontoberegning().orElseThrow().getStønadskontoer())
            .orElseThrow();
        var rettighetSegmenter = uttak.getGjeldendePerioder()
            .stream()
            .map(p -> new LocalDateSegment<>(p.getFom(), p.getTom(), RettighetUtleder.utledRettighet(p, yfa, konto)))
            .collect(Collectors.toSet());
        return new LocalDateTimeline<>(rettighetSegmenter)
            .compress(LocalDateInterval::abutsWorkdays, Objects::equals, StandardCombinators::leftOnly);
    }

    private void setUttaksresultatPerioder(UttakForeldrepenger uttakForeldrepenger, Behandling behandling) {
        var uttakResultat = uttakTjeneste.hentUttakHvisEksisterer(behandling.getId());
        uttakResultat.ifPresent(uttakResultatEntitet ->
            setUttakResultatPerioder(uttakForeldrepenger, uttakResultatEntitet.getGjeldendePerioder())
        );
    }

    private void setFordelingPerioder(UttakForeldrepenger uttakForeldrepenger, Behandling behandling) {
        ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId()).ifPresent(ytelseFordelingAggregat -> setUttakFordelingPerioder(uttakForeldrepenger, ytelseFordelingAggregat.getGjeldendeFordeling().getPerioder()));
    }

    private void setUttakFordelingPerioder(UttakForeldrepenger uttakForeldrepenger, List<OppgittPeriodeEntitet> perioderDomene) {
        var kontrakt = perioderDomene
            .stream()
            .map(this::konverterFraDomene)
            .toList();
        var fordelingPerioder = new UttakForeldrepenger.FordelingPerioder();
        fordelingPerioder.getFordelingPeriode().addAll(kontrakt);
        uttakForeldrepenger.setFordelingPerioder(fordelingPerioder);
    }

    private FordelingPeriode konverterFraDomene(OppgittPeriodeEntitet periodeDomene) {
        var kontrakt = new FordelingPeriode();
        kontrakt.setMorsAktivitet(VedtakXmlUtil.lagKodeverksOpplysning(periodeDomene.getMorsAktivitet()));
        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(periodeDomene.getFom(), periodeDomene.getTom()));
        kontrakt.setPeriodetype(VedtakXmlUtil.lagKodeverksOpplysning(periodeDomene.getPeriodeType()));
        return kontrakt;
    }

    private void setUttakResultatPerioder(UttakForeldrepenger uttakForeldrepenger, List<ForeldrepengerUttakPeriode> perioderDomene) {
        var kontrakt = perioderDomene
            .stream()
            .map(this::konverterFraDomene)
            .toList();
        uttakForeldrepenger.getUttaksresultatPerioder().addAll(kontrakt);
    }

    private UttaksresultatPeriode konverterFraDomene(ForeldrepengerUttakPeriode periodeDomene) {
        var kontrakt = new UttaksresultatPeriode();

        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(periodeDomene.getFom(), periodeDomene.getTom()));
        kontrakt.setPeriodeResultatType(VedtakXmlUtil.lagKodeverksOpplysning(periodeDomene.getResultatType()));
        kontrakt.setPerioderesultataarsak(VedtakXmlUtil.lagKodeverksOpplysning(periodeDomene.getResultatÅrsak()));
        Optional.ofNullable(periodeDomene.getManuellBehandlingÅrsak()).ifPresent(aarsak -> kontrakt.setManuellbehandlingaarsak(VedtakXmlUtil.lagKodeverksOpplysning(aarsak)));
        kontrakt.setBegrunnelse(VedtakXmlUtil.lagStringOpplysning(periodeDomene.getBegrunnelse()));
        setUttaksresultatPeriodeAktiviteter(kontrakt, periodeDomene.getAktiviteter());
        kontrakt.setGraderingInnvilget(VedtakXmlUtil.lagBooleanOpplysning(periodeDomene.isGraderingInnvilget()));
        kontrakt.setSamtidiguttak(VedtakXmlUtil.lagBooleanOpplysning(periodeDomene.isSamtidigUttak()));
        kontrakt.setUttakUtsettelseType(VedtakXmlUtil.lagKodeverksOpplysning(periodeDomene.getUtsettelseType()));
        kontrakt.setManueltBehandlet(VedtakXmlUtil.lagBooleanOpplysning(periodeDomene.isManueltBehandlet()));
        return kontrakt;
    }

    private void setUttaksresultatPeriodeAktiviteter(UttaksresultatPeriode uttaksresultatPeriodeKontrakt,
                                                     List<ForeldrepengerUttakPeriodeAktivitet> aktiviteterDomene) {
        var tidsperiode = uttaksresultatPeriodeKontrakt.getPeriode();
        var antVirkedager = Virkedager.beregnAntallVirkedager(tidsperiode.getFom(), tidsperiode.getTom());
        var resultat = aktiviteterDomene
            .stream()
            .map(periode -> konverterFraDomene(periode, antVirkedager))
            .toList();
        uttaksresultatPeriodeKontrakt.getUttaksresultatPeriodeAktiviteter().addAll(resultat);
    }

    private UttaksresultatPeriodeAktivitet konverterFraDomene(ForeldrepengerUttakPeriodeAktivitet periodeAktivitet, int antVirkedager) {
        var kontrakt = new UttaksresultatPeriodeAktivitet();
        kontrakt.setTrekkkonto(VedtakXmlUtil.lagKodeverksOpplysning(periodeAktivitet.getTrekkonto()));
        kontrakt.setTrekkdager(VedtakXmlUtil.lagDecimalOpplysning(periodeAktivitet.getTrekkdager().decimalValue()));
        periodeAktivitet.getArbeidsgiver().ifPresent(ag -> {
            kontrakt.setVirksomhet(VedtakXmlUtil.lagStringOpplysning(ag.getIdentifikator()));
            kontrakt.setArbeidsforholdid(VedtakXmlUtil.lagStringOpplysning(periodeAktivitet.getArbeidsforholdRef().getReferanse()));
        });
        kontrakt.setArbeidstidsprosent(VedtakXmlUtil.lagDecimalOpplysning(periodeAktivitet.getArbeidsprosent()));
        if(periodeAktivitet.getUtbetalingsgrad() != null) {
            kontrakt.setUtbetalingsprosent(VedtakXmlUtil.lagDecimalOpplysning(periodeAktivitet.getUtbetalingsgrad().decimalValue()));
        }

        kontrakt.setUttakarbeidtype(VedtakXmlUtil.lagKodeverksOpplysning(periodeAktivitet.getUttakArbeidType()));
        kontrakt.setGradering(VedtakXmlUtil.lagBooleanOpplysning(periodeAktivitet.isSøktGraderingForAktivitetIPeriode()));
        if(periodeAktivitet.isSøktGraderingForAktivitetIPeriode()) {
            kontrakt.setGraderingsdager(VedtakXmlUtil.lagIntOpplysning(antVirkedager));
        }
        return kontrakt;
    }

    private void setStoenadskontoer(UttakForeldrepenger uttakForeldrepenger, Behandling behandling) {
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak());
        if(fagsakRelasjon.isPresent()){
            var stønadskontoerOptional = fagsakRelasjon.get()
                .getGjeldendeStønadskontoberegning()
                .map(Stønadskontoberegning::getStønadskontoer);
            stønadskontoerOptional.ifPresent(stønadskontoer -> setStoenadskontoer(uttakForeldrepenger, stønadskontoer));
        }
    }

    private void setStoenadskontoer(UttakForeldrepenger uttakForeldrepenger, Set<Stønadskonto> stønadskontoerDomene) {
        var stønadskontoer = stønadskontoerDomene
            .stream()
            .map(this::konverterFraDomene)
            .toList();
        uttakForeldrepenger.getStoenadskontoer().addAll(stønadskontoer);
    }

    private Stoenadskonto konverterFraDomene(Stønadskonto stønadskontoDomene) {
        var stønadskonto = new Stoenadskonto();
        stønadskonto.setMaxdager(VedtakXmlUtil.lagIntOpplysning(stønadskontoDomene.getMaxDager()));
        stønadskonto.setStoenadskontotype(VedtakXmlUtil.lagKodeverksOpplysning(stønadskontoDomene.getStønadskontoType()));
        return stønadskonto;
    }

    private record BehRef(Saksnummer saksnummer, Long behandlingId, UUID behandlingUuid) {
    }
}
