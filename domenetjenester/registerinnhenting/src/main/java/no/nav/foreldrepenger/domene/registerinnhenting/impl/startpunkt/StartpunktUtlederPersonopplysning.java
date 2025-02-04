package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.dødsfall.BarnBorteEndringIdentifiserer;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

@ApplicationScoped
@GrunnlagRef(PersonInformasjonEntitet.ENTITY_NAME)
class StartpunktUtlederPersonopplysning implements StartpunktUtleder {

    private static final String BARNETS_DØDSDATO = "barnets dødsdato";

    private PersonopplysningRepository personopplysningRepository;
    private BehandlingRepository behandlingRepository;

    private BarnBorteEndringIdentifiserer barnBorteEndringIdentifiserer;
    private DekningsgradTjeneste dekningsgradTjeneste;

    StartpunktUtlederPersonopplysning() {
        // For CDI
    }

    @Inject
    public StartpunktUtlederPersonopplysning(PersonopplysningRepository personopplysningRepository,
                                             BehandlingRepository behandlingRepository,
                                             BarnBorteEndringIdentifiserer barnBorteEndringIdentifiserer,
                                             DekningsgradTjeneste dekningsgradTjeneste) {
        this.personopplysningRepository = personopplysningRepository;
        this.behandlingRepository = behandlingRepository;
        this.barnBorteEndringIdentifiserer = barnBorteEndringIdentifiserer;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2) {
        var grunnlag1 = personopplysningRepository.hentGrunnlagPåId((Long)grunnlagId1);
        var grunnlag2 = personopplysningRepository.hentGrunnlagPåId((Long)grunnlagId2);

        return hentAlleStartpunktForPersonopplysninger(ref, stp, grunnlag1, grunnlag2).stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    @Override
    public Set<StartpunktType> utledInitieltStartpunktRevurdering(BehandlingReferanse ref, Skjæringstidspunkt stp, Object grunnlagId1, Object grunnlagId2) {
        var grunnlag1 = personopplysningRepository.hentGrunnlagPåId((Long)grunnlagId1);
        var grunnlag2 = personopplysningRepository.hentGrunnlagPåId((Long)grunnlagId2);

        var ordinæreStartpunktTyper = new HashSet<>(hentAlleStartpunktForPersonopplysninger(ref, stp, grunnlag1, grunnlag2));
        if (!ordinæreStartpunktTyper.contains(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP) && endringssøknadManglerOppholdstillatelser(ref, stp)) {
            ordinæreStartpunktTyper.add(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP);
        }

        return ordinæreStartpunktTyper;
    }

    // Finn endringer per aggregat under grunnlaget og map dem mot startpunkt. Dekker bruker og PDL-relaterte personer (barn, ekte). Bør spisses der det er behov.
    private List<StartpunktType> hentAlleStartpunktForPersonopplysninger(BehandlingReferanse ref, Skjæringstidspunkt stp,
                                                                         PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2) {
        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
        var aktørId = ref.aktørId();

        var poDiff = new PersonopplysningGrunnlagDiff(aktørId, grunnlag1, grunnlag2);
        var uttaksIntervall = stp.getUttaksintervall().map(i -> DatoIntervallEntitet.fraOgMedTilOgMed(i.getFomDato(), i.getTomDato()))
            .orElseGet(() -> DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt));
        var forelderDødEndret = poDiff.erForeldreDødsdatoEndret();

        List<StartpunktType> startpunkter = new ArrayList<>();
        if (forelderDødEndret) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, "foreldres død", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.UTTAKSVILKÅR);
        }
        if (poDiff.erSivilstandEndretForBruker()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, "sivilstand", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.UTTAKSVILKÅR);
        }
        if (poDiff.erBarnDødsdatoEndret()) {
            if (ref.fagsakYtelseType() == FagsakYtelseType.FORELDREPENGER) {
                if (har80Dekningsgrad(ref)) {
                    FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.DEKNINGSGRAD, BARNETS_DØDSDATO, grunnlag1.getId(), grunnlag2.getId());
                    startpunkter.add(StartpunktType.DEKNINGSGRAD);
                } else {
                    FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, BARNETS_DØDSDATO, grunnlag1.getId(), grunnlag2.getId());
                    startpunkter.add(StartpunktType.UTTAKSVILKÅR);
                }
            } else {
                FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.BEREGNING, BARNETS_DØDSDATO, grunnlag1.getId(), grunnlag2.getId());
                startpunkter.add(StartpunktType.BEREGNING);
            }
        }

        if (poDiff.erPersonstatusIkkeBosattEndretForSøkerPeriode(uttaksIntervall)) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(),
                StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP, "personstatus ikke bosatt", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP);
        }
        if (poDiff.erSøkersUtlandsAdresserEndretIPeriode(uttaksIntervall)) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(),
                StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP, "utlandsadresse", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP);
        }

        if (barnBorteEndringIdentifiserer.erEndret(ref)) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.SØKERS_RELASJON_TIL_BARNET, "barn fjernet fra PDL", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.SØKERS_RELASJON_TIL_BARNET);
        }
        if (poDiff.erRelasjonerEndret() && !FagsakYtelseType.SVANGERSKAPSPENGER.equals(ref.fagsakYtelseType())) {
            leggTilForRelasjoner(grunnlag1.getId(), grunnlag2.getId(), poDiff, startpunkter);
        }
        if (startpunkter.isEmpty()) {
            // Endringen som trigget utledning av startpunkt skal ikke styre startpunkt
            var g1 = grunnlag1 != null ? grunnlag1.getId().toString() : "null";
            var g2 = grunnlag2 != null ? grunnlag2.getId().toString() : "null";
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UDEFINERT, "personopplysning - andre endringer", g1, g2);
            startpunkter.add(StartpunktType.UDEFINERT);
        }
        return startpunkter;
    }

    private boolean har80Dekningsgrad(BehandlingReferanse referanse) {
        return dekningsgradTjeneste.finnGjeldendeDekningsgradHvisEksisterer(referanse)
            .filter(Dekningsgrad._80::equals)
            .isPresent();
    }

    private void leggTilForRelasjoner(Long g1Id, Long g2Id, PersonopplysningGrunnlagDiff poDiff, List<StartpunktType> startpunkter) {
        if (poDiff.erRelasjonerEndretSøkerAntallBarn()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UDEFINERT, "personopplysning - relasjon på grunn av fødsel", g1Id, g2Id);
            startpunkter.add(StartpunktType.UDEFINERT);
        }
        if (poDiff.erRelasjonerEndretForSøkerUtenomNyeBarn()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.SØKERS_RELASJON_TIL_BARNET, "personopplysning - brukers relasjoner annet enn fødsel", g1Id, g2Id);
            startpunkter.add(StartpunktType.SØKERS_RELASJON_TIL_BARNET);
        }
        if (poDiff.erRelasjonerEndretForEksisterendeBarn()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.SØKERS_RELASJON_TIL_BARNET, "personopplysning - barns relasjoner annet enn fødsel", g1Id, g2Id);
            startpunkter.add(StartpunktType.SØKERS_RELASJON_TIL_BARNET);
        }
        if (poDiff.erRelasjonerBostedEndretForSøkerUtenomNyeBarn()) {
            // Endring i harsammebosted -> omsorgsvurdering - med mindre en av de nedenfor slår til
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, "personopplysning - relasjoner bosted eller ektefelle", g1Id, g2Id);
            startpunkter.add(StartpunktType.UTTAKSVILKÅR);
        }
    }

    private boolean endringssøknadManglerOppholdstillatelser(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        if (ref.erRevurdering() && behandlingRepository.hentBehandling(ref.behandlingId()).harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
            var aktivtgrunnlag = personopplysningRepository.hentPersonopplysninger(ref.behandlingId());
            var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
            var periode = stp.getUttaksintervall().map(i -> DatoIntervallEntitet.fraOgMedTilOgMed(i.getFomDato(), i.getTomDato()))
                .orElseGet(() -> DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt));
            var tredjeland = getRegionIntervaller(aktivtgrunnlag, ref.aktørId(), periode, skjæringstidspunkt).filterValue(Region.TREDJELANDS_BORGER::equals);
            if (!tredjeland.isEmpty()) {
                var oppholdstillatelser = aktivtgrunnlag.getRegisterVersjon().map(PersonInformasjonEntitet::getOppholdstillatelser).orElse(List.of()).stream()
                    .filter(o -> !OppholdstillatelseType.UDEFINERT.equals(o.getTillatelse()))
                    .filter(o -> ref.aktørId().equals(o.getAktørId()))
                    .filter(o -> periode.overlapper(o.getPeriode()))
                    .map(o -> new LocalDateSegment<>(o.getPeriode().getFomDato(), o.getPeriode().getTomDato(), Boolean.TRUE))
                    .collect(Collectors.collectingAndThen(Collectors.toSet(), s -> new LocalDateTimeline<>(s, StandardCombinators::alwaysTrueForMatch)));

                return !tredjeland.disjoint(oppholdstillatelser).isEmpty();
            }
        }
        return false;
    }

    private LocalDateTimeline<Region> getRegionIntervaller(PersonopplysningGrunnlagEntitet grunnlag, AktørId person, AbstractLocalDateInterval interval, LocalDate skjæringstidspunkt) {
        return grunnlag.getRegisterVersjon().map(PersonInformasjonEntitet::getStatsborgerskap).orElse(List.of()).stream()
            .filter(stb -> person.equals(stb.getAktørId()))
            .filter(s -> s.getPeriode().overlapper(interval))
            .map(s -> {
                var region = MapRegionLandkoder.mapLandkodeForDatoMedSkjæringsdato(s.getStatsborgerskap(), interval.getFomDato(), skjæringstidspunkt);
                return new LocalDateSegment<>(new LocalDateInterval(s.getPeriode().getFomDato(), s.getPeriode().getTomDato()), region);
            })
            .collect(Collectors.collectingAndThen(Collectors.toSet(), s -> new LocalDateTimeline<>(s, this::prioritertRegion)));
    }

    private LocalDateSegment<Region> prioritertRegion(LocalDateInterval i, LocalDateSegment<Region> s1, LocalDateSegment<Region> s2) {
        var prioritertRegion = Region.COMPARATOR.compare(s1.getValue(), s2.getValue()) < 0 ? s1.getValue() : s2.getValue();
        return new LocalDateSegment<>(i, prioritertRegion);
    }

}
