package no.nav.foreldrepenger.domene.medlem;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.domene.medlem.api.VurderingsÅrsak;
import no.nav.foreldrepenger.domene.medlem.impl.MedlemEndringssjekker;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class UtledVurderingsdatoerForMedlemskapTjeneste {

    private Instance<MedlemEndringssjekker> alleEndringssjekkere;
    private MedlemskapRepository medlemskapRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    public UtledVurderingsdatoerForMedlemskapTjeneste(MedlemskapRepository medlemskapRepository,
                                                      @Any Instance<MedlemEndringssjekker> alleEndringssjekkere,
                                                      PersonopplysningTjeneste personopplysningTjeneste) {
        this.alleEndringssjekkere = alleEndringssjekkere;
        this.medlemskapRepository = medlemskapRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    UtledVurderingsdatoerForMedlemskapTjeneste() {
        //CDI
    }

    /** Utleder vurderingsdatoer for:
     * - utledVurderingsdatoerForPersonopplysninger
     * - utledVurderingsdatoerForBortfallAvInntekt
     * - utledVurderingsdatoerForMedlemskap
     *
     * Ser bare på datoer etter skjæringstidspunktet
     * @param ref behandlingreferanse
     * @return datoer med diff i medlemskap
     */
    public Set<LocalDate> finnVurderingsdatoer(BehandlingReferanse ref) {
        var endringssjekker = FagsakYtelseTypeRef.Lookup.find(alleEndringssjekkere, ref.fagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + ref.fagsakYtelseType().getKode()));

        Set<LocalDate> datoer = new HashSet<>();

        datoer.addAll(utledVurderingsdatoerForTPS(ref).keySet());
        datoer.addAll(utledVurderingsdatoerForMedlemskap(ref.behandlingId(), endringssjekker).keySet());

        // ønsker bare å se på datoer etter skjæringstidspunktet
        return datoer.stream().filter(d -> d.isAfter(ref.getUtledetSkjæringstidspunkt()) && ref.getUtledetMedlemsintervall().encloses(d)).collect(Collectors.toSet());
    }

    public Set<LocalDate> finnVurderingsDatoerForutForStpEngangsstønad(BehandlingReferanse ref) {
        var endringssjekker = FagsakYtelseTypeRef.Lookup.find(alleEndringssjekkere, ref.fagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + ref.fagsakYtelseType().getKode()));
        var fom = ref.getUtledetSkjæringstidspunkt().minusMonths(12);

        Set<LocalDate> datoer = new HashSet<>();

        datoer.addAll(utledVurderingsdatoerForTPS(ref, fom, ref.getUtledetSkjæringstidspunkt()).keySet());
        datoer.addAll(utledVurderingsdatoerForMedlemskap(ref.behandlingId(), endringssjekker).keySet());
        datoer.add(fom);

        return datoer.stream().filter(d -> d.isBefore(ref.getUtledetSkjæringstidspunkt())).filter(d -> !d.isBefore(fom)).collect(Collectors.toSet());
    }

    Map<LocalDate, Set<VurderingsÅrsak>> finnVurderingsdatoerMedÅrsak(BehandlingReferanse ref) {
        var endringssjekker = FagsakYtelseTypeRef.Lookup.find(alleEndringssjekkere, ref.fagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Ingen implementasjoner funnet for ytelse: " + ref.fagsakYtelseType().getKode()));
        Map<LocalDate, Set<VurderingsÅrsak>> datoer = new HashMap<>();

        //kan ikke gjøre en vurdering hvis det ikke er en revurderingkontekts
        if (!BehandlingType.REVURDERING.equals(ref.behandlingType())) {
            return datoer;
        }
        var utledetSkjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();
        datoer.putAll(utledVurderingsdatoerForTPS(ref));
        datoer.putAll(utledVurderingsdatoerForMedlemskap(ref.behandlingId(), endringssjekker));

        // ønsker bare å se på datoer etter skjæringstidspunktet
        return datoer.entrySet().stream()
            .filter(entry -> entry.getKey().isAfter(utledetSkjæringstidspunkt) && ref.getUtledetMedlemsintervall().encloses(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<LocalDate, Set<VurderingsÅrsak>> utledVurderingsdatoerForTPS(BehandlingReferanse ref) {
        return utledVurderingsdatoerForTPS(ref, ref.getUtledetMedlemsintervall().getFomDato(), ref.getUtledetMedlemsintervall().getTomDato());
    }

    private Map<LocalDate, Set<VurderingsÅrsak>> utledVurderingsdatoerForTPS(BehandlingReferanse ref, LocalDate fom, LocalDate tom) {
        final Map<LocalDate, Set<VurderingsÅrsak>> utledetResultat = new HashMap<>();
        var relevantPeriodeEntitet = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);

        var personopplysningerOpt = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref);
        if (personopplysningerOpt.isPresent()) {
            var personopplysningerAggregat = personopplysningerOpt.get();

            utledetResultat.putAll(hentEndringForStatsborgerskap(personopplysningerAggregat, ref, relevantPeriodeEntitet));
            mergeResultat(utledetResultat, hentEndringForPersonstatus(personopplysningerAggregat, ref, relevantPeriodeEntitet));
            mergeResultat(utledetResultat, hentEndringForAdresse(personopplysningerAggregat, ref, relevantPeriodeEntitet));
            mergeResultat(utledetResultat, hentEndringForOppholdstillatelse(ref));
        }
        return utledetResultat;
    }

    private void mergeResultat(Map<LocalDate, Set<VurderingsÅrsak>> utledetResultat, Map<LocalDate, Set<VurderingsÅrsak>> nyeEndringer) {
        for (var localDateSetEntry : nyeEndringer.entrySet()) {
            utledetResultat.merge(localDateSetEntry.getKey(), localDateSetEntry.getValue(), this::slåSammenSet);
        }
    }

    private Set<VurderingsÅrsak> slåSammenSet(Set<VurderingsÅrsak> value1, Set<VurderingsÅrsak> value2) {
        Set<VurderingsÅrsak> vurderingsÅrsaks = new HashSet<>(value1);
        vurderingsÅrsaks.addAll(value2);
        return vurderingsÅrsaks;
    }

    private Map<LocalDate, Set<VurderingsÅrsak>> utledVurderingsdatoerForMedlemskap(Long revurderingId, MedlemEndringssjekker endringssjekker) {
        var førsteVersjon = medlemskapRepository.hentFørsteVersjonAvMedlemskap(revurderingId);
        var sisteVersjon = medlemskapRepository.hentMedlemskap(revurderingId);

        var første = førsteVersjon.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder).orElse(Collections.emptySet());
        var siste = sisteVersjon.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder).orElse(Collections.emptySet());

        var førsteListe = første.stream().map(r -> new LocalDateSegment<>(r.getFom(), r.getTom(), r)).toList();
        var sisteListe = siste.stream().map(r -> new LocalDateSegment<>(r.getFom(), r.getTom(), r)).toList();

        var førsteTidsserie = new LocalDateTimeline<MedlemskapPerioderEntitet>(førsteListe, this::slåSammenMedlemskapPerioder);
        var andreTidsserie = new LocalDateTimeline<MedlemskapPerioderEntitet>(sisteListe, this::slåSammenMedlemskapPerioder);

        var resultat = førsteTidsserie.combine(andreTidsserie, (di, førsteVersjon1, sisteVersjon1) -> sjekkForEndringIMedl(di, førsteVersjon1, sisteVersjon1, endringssjekker), LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return utledResultat(resultat);
    }

    private Map<LocalDate, Set<VurderingsÅrsak>> utledResultat(LocalDateTimeline<MedlemskapPerioderEntitet> resultat) {
        final Map<LocalDate, Set<VurderingsÅrsak>> utledetResultat = new HashMap<>();
        var datoIntervaller = resultat.getLocalDateIntervals();
        for (var localDateInterval : datoIntervaller) {
            var perioden = resultat.getSegment(localDateInterval);

            utledetResultat.put(perioden.getFom(), Set.of(VurderingsÅrsak.MEDL_PERIODE));
        }
        return utledetResultat;
    }

    // PDL gir en del samtidige adresser
    private Map<LocalDate, Set<VurderingsÅrsak>> hentEndringForAdresse(PersonopplysningerAggregat personopplysningerAggregat,
                                                                       BehandlingReferanse ref, AbstractLocalDateInterval intervall) {
        var adresseSegmenter = personopplysningerAggregat.getAdresserFor(ref.aktørId(), intervall).stream()
            .filter(PersonAdresseEntitet::erUtlandskAdresse)
            .map(PersonAdresseEntitet::getPeriode)
            .toList();
        var fomdatoer = adresseSegmenter.stream().map(DatoIntervallEntitet::getFomDato).collect(Collectors.toSet());
        final Map<LocalDate, Set<VurderingsÅrsak>> utledetResultat = new HashMap<>();

        adresseSegmenter.forEach(periode -> {
                utledetResultat.put(periode.getFomDato(), Set.of(VurderingsÅrsak.ADRESSE));
                if (!Tid.TIDENES_ENDE.equals(periode.getTomDato()) && !fomdatoer.contains(periode.getTomDato().plusDays(1))) {
                    utledetResultat.put(periode.getTomDato().plusDays(1), Set.of(VurderingsÅrsak.ADRESSE));
                }
            });

        return utledetResultat;
    }

    // PDL gir strengt periodisert informasjon om personstatus
    private Map<LocalDate, Set<VurderingsÅrsak>> hentEndringForPersonstatus(PersonopplysningerAggregat personopplysningerAggregat,
                                                                            BehandlingReferanse ref, AbstractLocalDateInterval intervall) {
        var personstatus = personopplysningerAggregat.getPersonstatuserFor(ref.aktørId(), intervall)
            .stream().sorted(Comparator.comparing(s -> s.getPeriode().getFomDato()))
            .toList();
        final Map<LocalDate, Set<VurderingsÅrsak>> utledetResultat = new HashMap<>();
        IntStream.range(0, personstatus.size() - 1).forEach(i -> {
            if (i != personstatus.size() - 1) { // sjekker om det er siste element
                var førsteElement = personstatus.get(i);
                var nesteElement = personstatus.get(i + 1);
                //skal ikke trigge på personstaus død
                var personStausInneholderDød = PersonstatusType.erDød(førsteElement.getPersonstatus()) || PersonstatusType.erDød(nesteElement.getPersonstatus());
                if (!personStausInneholderDød && !førsteElement.getPersonstatus().equals(nesteElement.getPersonstatus())) {
                    utledetResultat.put(nesteElement.getPeriode().getFomDato(), Set.of(VurderingsÅrsak.PERSONSTATUS));
                }
            }
        });
        return utledetResultat;
    }

    // PDL har flere samtidige statsborgerskap - dermed må man sjekke region ved hvert brudd
    private Map<LocalDate, Set<VurderingsÅrsak>> hentEndringForStatsborgerskap(PersonopplysningerAggregat aggregat, BehandlingReferanse ref, AbstractLocalDateInterval intervall) {
        var statsborgerskapene = aggregat.getStatsborgerskapFor(ref.aktørId(), intervall);
        var statsborgerskapDatoer = statsborgerskapene.stream()
            .map(StatsborgerskapEntitet::getPeriode).map(DatoIntervallEntitet::getFomDato)
            .collect(Collectors.toSet());
        var statsborgerskapLand = statsborgerskapene.stream().map(StatsborgerskapEntitet::getStatsborgerskap).toList();
        statsborgerskapDatoer.addAll(MapRegionLandkoder.utledRegionsEndringsDatoer(statsborgerskapLand));
        var statsborgerskap = statsborgerskapDatoer.stream().sorted(Comparator.naturalOrder()).toList();
        final Map<LocalDate, Set<VurderingsÅrsak>> utledetResultat = new HashMap<>();
        var max = statsborgerskap.size() - 1;
        IntStream.range(0, max).forEach(i -> {
            if (i != max) { // sjekker om det er siste element
                var førsteElement = statsborgerskap.get(i);
                var nesteElement = statsborgerskap.get(i + 1);
                if (!aggregat.getStatsborgerskapRegionVedTidspunkt(ref.aktørId(), førsteElement, ref.getUtledetSkjæringstidspunkt())
                    .equals(aggregat.getStatsborgerskapRegionVedTidspunkt(ref.aktørId(), nesteElement, ref.getUtledetSkjæringstidspunkt()))) {
                    utledetResultat.put(nesteElement, Set.of(VurderingsÅrsak.STATSBORGERSKAP));
                }
            }
        });
        return utledetResultat;
    }

    // Dagen etter opphør av midlertidige oppholdstillatelser
    private Map<LocalDate, Set<VurderingsÅrsak>> hentEndringForOppholdstillatelse(BehandlingReferanse ref) {
        return personopplysningTjeneste.hentOppholdstillatelseTidslinje(ref.behandlingId()).getLocalDateIntervals().stream()
            .map(LocalDateInterval::getTomDato)
            .filter(d -> !Tid.TIDENES_ENDE.equals(d))
            .map(d -> new AbstractMap.SimpleEntry<>(d.plusDays(1), Set.of(VurderingsÅrsak.OPPHOLDSTILLATELSE)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private LocalDateSegment<MedlemskapPerioderEntitet> sjekkForEndringIMedl(@SuppressWarnings("unused") LocalDateInterval di,
                                                                             LocalDateSegment<MedlemskapPerioderEntitet> førsteVersjon,
                                                                             LocalDateSegment<MedlemskapPerioderEntitet> sisteVersjon, MedlemEndringssjekker endringssjekker) {

        // må alltid sjekke datoer med overlapp
        if (førsteVersjon != null && førsteVersjon.getValue().getKildeType() == null) {
            return førsteVersjon;
        }

        // må alltid sjekke datoer med overlapp
        if (sisteVersjon != null && sisteVersjon.getValue().getKildeType() == null) {
            return sisteVersjon;
        }

        // ny periode registrert
        if (førsteVersjon == null) {
            return sisteVersjon;
        }
        if (sisteVersjon != null) {
            // sjekker om gammel periode har endret verdier
            if (endringssjekker.erEndring(førsteVersjon.getValue(), sisteVersjon.getValue())) {
                return sisteVersjon;
            }
            return null;
        }
        // gammel periode fjernet
        return førsteVersjon;
    }

    // Ligger her som en guard mot dårlig datakvalitet i medl.. Skal nemlig aldri inntreffe
    private LocalDateSegment<MedlemskapPerioderEntitet> slåSammenMedlemskapPerioder(LocalDateInterval di,
                                                                                       LocalDateSegment<MedlemskapPerioderEntitet> førsteVersjon,
                                                                                       LocalDateSegment<MedlemskapPerioderEntitet> sisteVersjon) {
        if (førsteVersjon == null) {
            return sisteVersjon;
        }
        if (sisteVersjon == null) {
            return førsteVersjon;
        }

        var senesteBesluttetPeriode = finnMedlemskapPeriodeMedSenestBeslutningsdato(førsteVersjon, sisteVersjon);
        var builder = new MedlemskapPerioderBuilder(senesteBesluttetPeriode);
        builder.medPeriode(di.getFomDato(), di.getTomDato());
        builder.medKildeType(senesteBesluttetPeriode.getKildeType());

        return new LocalDateSegment<>(di.getFomDato(), di.getTomDato(), builder.build());
    }

    private MedlemskapPerioderEntitet finnMedlemskapPeriodeMedSenestBeslutningsdato(LocalDateSegment<MedlemskapPerioderEntitet> førsteVersjon, LocalDateSegment<MedlemskapPerioderEntitet> sisteVersjon) {
        MedlemskapPerioderEntitet riktigEntitetVerdi;
        var førsteBeslutningsdato = førsteVersjon.getValue().getBeslutningsdato();
        var sisteBeslutningsdato = sisteVersjon.getValue().getBeslutningsdato();
        if (førsteBeslutningsdato != null && (sisteBeslutningsdato == null || førsteBeslutningsdato.isAfter(sisteBeslutningsdato))) {
            riktigEntitetVerdi = førsteVersjon.getValue();
        } else {
            riktigEntitetVerdi = sisteVersjon.getValue();
        }
        return riktigEntitetVerdi;
    }

}
