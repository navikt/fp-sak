package no.nav.foreldrepenger.domene.vedtak.fp;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.Stønadskontoberegning;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.regler.uttak.felles.Virkedager;
import no.nav.vedtak.felles.xml.vedtak.uttak.fp.v2.FordelingPeriode;
import no.nav.vedtak.felles.xml.vedtak.uttak.fp.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.uttak.fp.v2.Stoenadskonto;
import no.nav.vedtak.felles.xml.vedtak.uttak.fp.v2.UttakForeldrepenger;
import no.nav.vedtak.felles.xml.vedtak.uttak.fp.v2.UttaksresultatPeriode;
import no.nav.vedtak.felles.xml.vedtak.uttak.fp.v2.UttaksresultatPeriodeAktivitet;
import no.nav.vedtak.felles.xml.vedtak.v2.Beregningsresultat;
import no.nav.vedtak.felles.xml.vedtak.v2.Uttak;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class UttakXmlTjenesteImpl {

    private ObjectFactory uttakObjectFactory;
    private UttakRepository uttakRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    public UttakXmlTjenesteImpl() {
        //For CDI
    }

    @Inject
    public UttakXmlTjenesteImpl(BehandlingRepositoryProvider repositoryProvider) {
        this.uttakObjectFactory = new ObjectFactory();
        this.uttakRepository = repositoryProvider.getUttakRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    }

    public void setUttak(Beregningsresultat beregningsresultat, Behandling behandling) {
        UttakForeldrepenger uttakForeldrepenger = uttakObjectFactory.createUttakForeldrepenger();

        Optional<Uttaksperiodegrense> uttaksperiodegrenseOptional = uttakRepository.hentUttaksperiodegrenseHvisEksisterer(behandling.getId());
        uttaksperiodegrenseOptional.ifPresent(uttaksperiodegrense ->
            VedtakXmlUtil.lagDateOpplysning(uttaksperiodegrense.getFørsteLovligeUttaksdag()).ifPresent(dateOpplysning -> uttakForeldrepenger.setFoersteLovligeUttaksdag(dateOpplysning)));

        setStoenadskontoer(uttakForeldrepenger, behandling);
        setUttaksresultatPerioder(uttakForeldrepenger, behandling);
        setFordelingPerioder(uttakForeldrepenger, behandling);

        Uttak uttak = new Uttak();
        uttak.getAny().add(uttakObjectFactory.createUttak(uttakForeldrepenger));
        beregningsresultat.setUttak(uttak);
    }

    private void setUttaksresultatPerioder(UttakForeldrepenger uttakForeldrepenger, Behandling behandling) {
        Optional<UttakResultatEntitet> uttakResultat = uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        uttakResultat.ifPresent(uttakResultatEntitet ->
            setUttakResultatPerioder(uttakForeldrepenger, uttakResultatEntitet.getGjeldendePerioder().getPerioder())
        );
    }

    private void setFordelingPerioder(UttakForeldrepenger uttakForeldrepenger, Behandling behandling) {
        ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandling.getId()).ifPresent(ytelseFordelingAggregat -> setUttakFordelingPerioder(uttakForeldrepenger, ytelseFordelingAggregat.getGjeldendeSøknadsperioder().getOppgittePerioder()));
    }

    private void setUttakFordelingPerioder(UttakForeldrepenger uttakForeldrepenger, List<OppgittPeriodeEntitet> perioderDomene) {
        List<FordelingPeriode> kontrakt = perioderDomene
            .stream()
            .map(fordelingPeriode -> konverterFraDomene(fordelingPeriode)).collect(Collectors.toList());
        UttakForeldrepenger.FordelingPerioder fordelingPerioder = new UttakForeldrepenger.FordelingPerioder();
        fordelingPerioder.getFordelingPeriode().addAll(kontrakt);
        uttakForeldrepenger.setFordelingPerioder(fordelingPerioder);
    }

    private FordelingPeriode konverterFraDomene(OppgittPeriodeEntitet periodeDomene) {
        FordelingPeriode kontrakt = new FordelingPeriode();
        kontrakt.setMorsAktivitet(VedtakXmlUtil.lagKodeverksOpplysning(periodeDomene.getMorsAktivitet()));
        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(periodeDomene.getFom(), periodeDomene.getTom()));
        kontrakt.setPeriodetype(VedtakXmlUtil.lagKodeverksOpplysning(periodeDomene.getPeriodeType()));
        return kontrakt;
    }

    private void setUttakResultatPerioder(UttakForeldrepenger uttakForeldrepenger, List<UttakResultatPeriodeEntitet> perioderDomene) {
        List<UttaksresultatPeriode> kontrakt = perioderDomene
            .stream()
            .map(periode -> konverterFraDomene(periode)).collect(Collectors.toList());
        uttakForeldrepenger.getUttaksresultatPerioder().addAll(kontrakt);
    }

    private UttaksresultatPeriode konverterFraDomene(UttakResultatPeriodeEntitet periodeDomene) {
        UttaksresultatPeriode kontrakt = new UttaksresultatPeriode();

        kontrakt.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(periodeDomene.getFom(), periodeDomene.getTom()));
        kontrakt.setPeriodeResultatType(VedtakXmlUtil.lagKodeverksOpplysning(periodeDomene.getPeriodeResultatType()));
        kontrakt.setPerioderesultataarsak(VedtakXmlUtil.lagKodeverksOpplysning(periodeDomene.getPeriodeResultatÅrsak()));
        Optional.ofNullable(periodeDomene.getManuellBehandlingÅrsak()).ifPresent(aarsak -> kontrakt.setManuellbehandlingaarsak(VedtakXmlUtil.lagKodeverksOpplysning(aarsak)));
        kontrakt.setBegrunnelse(VedtakXmlUtil.lagStringOpplysning(periodeDomene.getBegrunnelse()));
        setUttaksresultatPeriodeAktiviteter(kontrakt, periodeDomene.getAktiviteter());
        kontrakt.setGraderingInnvilget(VedtakXmlUtil.lagBooleanOpplysning(periodeDomene.isGraderingInnvilget()));
        kontrakt.setSamtidiguttak(VedtakXmlUtil.lagBooleanOpplysning(periodeDomene.isSamtidigUttak()));
        kontrakt.setUttakUtsettelseType(VedtakXmlUtil.lagKodeverksOpplysning(periodeDomene.getUtsettelseType()));
        kontrakt.setManueltBehandlet(VedtakXmlUtil.lagBooleanOpplysning(periodeDomene.isManueltBehandlet()));
        return kontrakt;
    }

    private void setUttaksresultatPeriodeAktiviteter(UttaksresultatPeriode uttaksresultatPeriodeKontrakt, List<UttakResultatPeriodeAktivitetEntitet> aktiviteterDomene) {
        List<UttaksresultatPeriodeAktivitet> resultat = aktiviteterDomene
            .stream()
            .map(periode -> konverterFraDomene(periode)).collect(Collectors.toList());
        uttaksresultatPeriodeKontrakt.getUttaksresultatPeriodeAktiviteter().addAll(resultat);
    }

    private UttaksresultatPeriodeAktivitet konverterFraDomene(UttakResultatPeriodeAktivitetEntitet periodeAktivitet) {
        UttaksresultatPeriodeAktivitet kontrakt = new UttaksresultatPeriodeAktivitet();
        kontrakt.setTrekkkonto(VedtakXmlUtil.lagKodeverksOpplysning(periodeAktivitet.getTrekkonto()));
        kontrakt.setTrekkdager(VedtakXmlUtil.lagDecimalOpplysning(periodeAktivitet.getTrekkdager().decimalValue()));
        kontrakt.setVirksomhet(VedtakXmlUtil.lagStringOpplysning(periodeAktivitet.getArbeidsgiverIdentifikator()));
        kontrakt.setArbeidsforholdid(VedtakXmlUtil.lagStringOpplysning(periodeAktivitet.getArbeidsforholdId()));
        kontrakt.setArbeidstidsprosent(VedtakXmlUtil.lagDecimalOpplysning(periodeAktivitet.getArbeidsprosent()));
        if(periodeAktivitet.getUtbetalingsprosent()!=null) {
            kontrakt.setUtbetalingsprosent(VedtakXmlUtil.lagDecimalOpplysning(periodeAktivitet.getUtbetalingsprosent()));
        }
        kontrakt.setUttakarbeidtype(VedtakXmlUtil.lagKodeverksOpplysning(periodeAktivitet.getUttakArbeidType()));
        kontrakt.setGradering(VedtakXmlUtil.lagBooleanOpplysning(periodeAktivitet.isSøktGradering()));
        int antVirkedager = Virkedager.beregnAntallVirkedager(periodeAktivitet.getFom(), periodeAktivitet.getTom());
        if(periodeAktivitet.isSøktGradering()) {
            kontrakt.setGraderingsdager(VedtakXmlUtil.lagIntOpplysning(antVirkedager));
        }
        return kontrakt;
    }

    private void setStoenadskontoer(UttakForeldrepenger uttakForeldrepenger, Behandling behandling) {
        Optional<FagsakRelasjon> fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonForHvisEksisterer(behandling.getFagsak());
        if(fagsakRelasjon.isPresent()){
            Optional<Set<Stønadskonto>> stønadskontoerOptional = fagsakRelasjon.get()
                .getGjeldendeStønadskontoberegning()
                .map(Stønadskontoberegning::getStønadskontoer);
            stønadskontoerOptional.ifPresent(stønadskontoer -> setStoenadskontoer(uttakForeldrepenger, stønadskontoer));
        }
    }

    private void setStoenadskontoer(UttakForeldrepenger uttakForeldrepenger, Set<Stønadskonto> stønadskontoerDomene) {
        List<Stoenadskonto> stønadskontoer = stønadskontoerDomene
            .stream()
            .map(konto -> konverterFraDomene(konto)).collect(Collectors.toList());
        uttakForeldrepenger.getStoenadskontoer().addAll(stønadskontoer);
    }

    private Stoenadskonto konverterFraDomene(Stønadskonto stønadskontoDomene) {
        Stoenadskonto stønadskonto = new Stoenadskonto();
        stønadskonto.setMaxdager(VedtakXmlUtil.lagIntOpplysning(stønadskontoDomene.getMaxDager()));
        stønadskonto.setStoenadskontotype(VedtakXmlUtil.lagKodeverksOpplysning(stønadskontoDomene.getStønadskontoType()));
        return stønadskonto;
    }
}
