package no.nav.foreldrepenger.datavarehus.xml.fp;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.datavarehus.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.UtregnetStønadskontoTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;
import no.nav.vedtak.felles.xml.felles.v2.KodeverksOpplysning;
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

    private ObjectFactory uttakObjectFactory;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private UtregnetStønadskontoTjeneste utregnetStønadskontoTjeneste;
    private YtelsesFordelingRepository ytelsesFordelingRepository;

    public UttakXmlTjeneste() {
        //For CDI
    }

    @Inject
    public UttakXmlTjeneste(BehandlingRepositoryProvider repositoryProvider,
                            ForeldrepengerUttakTjeneste uttakTjeneste,
                            UtregnetStønadskontoTjeneste utregnetStønadskontoTjeneste) {
        this.uttakObjectFactory = new ObjectFactory();
        this.uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();
        this.utregnetStønadskontoTjeneste = utregnetStønadskontoTjeneste;
        this.ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        this.uttakTjeneste = uttakTjeneste;
    }

    public void setUttak(Beregningsresultat beregningsresultat, Behandling behandling) {
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

    private void setUttaksresultatPerioder(UttakForeldrepenger uttakForeldrepenger, Behandling behandling) {
        var uttakResultat = uttakTjeneste.hentHvisEksisterer(behandling.getId());
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
        kontrakt.setPeriodetype(periodeDomene.isOpphold() ? lagOppholdPeriodeTypeKodeverkOpplysning()
            : VedtakXmlUtil.lagKodeverksOpplysning(periodeDomene.getPeriodeType()));
        return kontrakt;
    }

    private static KodeverksOpplysning lagOppholdPeriodeTypeKodeverkOpplysning() {
        //Annet fjernet fra enum
        return VedtakXmlUtil.lagKodeverksOpplysning("ANNET", "Andre typer som f.eks utsettelse", "UTTAK_PERIODE_TYPE");
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
        kontrakt.setTrekkkonto(lagKodeverksOpplysningFor(periodeAktivitet.getTrekkonto()));
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

    private static KodeverksOpplysning lagKodeverksOpplysningFor(UttakPeriodeType trekkonto) {
        if (trekkonto == UttakPeriodeType.UDEFINERT) {
            //StønadskontoType.UDEFINERT fjernet
            return VedtakXmlUtil.lagKodeverksOpplysning("-", "Ikke valgt stønadskonto", trekkonto.getClass().getSimpleName());
        }
        var kodeverdi = switch (trekkonto) {
            case FELLESPERIODE -> StønadskontoType.FELLESPERIODE;
            case MØDREKVOTE -> StønadskontoType.MØDREKVOTE;
            case FEDREKVOTE -> StønadskontoType.FEDREKVOTE;
            case FORELDREPENGER -> StønadskontoType.FORELDREPENGER;
            case FORELDREPENGER_FØR_FØDSEL -> StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
            case UDEFINERT -> throw new IllegalStateException("UttakPeriodeType.UDEFINERT kan ikke mappes til StønadskontoType");
        };
        return VedtakXmlUtil.lagKodeverksOpplysning(kodeverdi);
    }

    private void setStoenadskontoer(UttakForeldrepenger uttakForeldrepenger, Behandling behandling) {
        var kontoutregning = utregnetStønadskontoTjeneste.gjeldendeKontoutregning(BehandlingReferanse.fra(behandling));
        if(!kontoutregning.isEmpty()){
            setStoenadskontoer(uttakForeldrepenger, kontoutregning);
        }
    }

    private void setStoenadskontoer(UttakForeldrepenger uttakForeldrepenger, Map<StønadskontoType, Integer> stønadskontoerDomene) {
        var stønadskontoer = stønadskontoerDomene.entrySet()
            .stream()
            .filter(s -> s.getKey().erStønadsdager() || StønadskontoType.FLERBARNSDAGER.equals(s.getKey()))
            .map(this::konverterFraDomene)
            .toList();
        uttakForeldrepenger.getStoenadskontoer().addAll(stønadskontoer);
    }

    private Stoenadskonto konverterFraDomene(Map.Entry<StønadskontoType, Integer> stønadskontoDomene) {
        var stønadskonto = new Stoenadskonto();
        stønadskonto.setMaxdager(VedtakXmlUtil.lagIntOpplysning(stønadskontoDomene.getValue()));
        stønadskonto.setStoenadskontotype(VedtakXmlUtil.lagKodeverksOpplysning(stønadskontoDomene.getKey()));
        return stønadskonto;
    }
}
