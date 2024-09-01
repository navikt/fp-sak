package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.familiehendelse.dødsfall.BarnBorteEndringIdentifiserer;

@ApplicationScoped
@GrunnlagRef(PersonInformasjonEntitet.ENTITY_NAME)
class StartpunktUtlederPersonopplysning implements StartpunktUtleder {

    private PersonopplysningRepository personopplysningRepository;

    private BarnBorteEndringIdentifiserer barnBorteEndringIdentifiserer;
    private DekningsgradTjeneste dekningsgradTjeneste;

    StartpunktUtlederPersonopplysning() {
        // For CDI
    }

    @Inject
    StartpunktUtlederPersonopplysning(PersonopplysningRepository personopplysningRepository,
                                      BarnBorteEndringIdentifiserer barnBorteEndringIdentifiserer,
                                      DekningsgradTjeneste dekningsgradTjeneste) {
        this.personopplysningRepository = personopplysningRepository;
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

    // Finn endringer per aggregat under grunnlaget og map dem mot startpunkt. Dekker bruker og PDL-relaterte personer (barn, ekte). Bør spisses der det er behov.
    private List<StartpunktType> hentAlleStartpunktForPersonopplysninger(BehandlingReferanse ref, Skjæringstidspunkt stp,
                                                                         PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2) {
        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
        var aktørId = ref.aktørId();

        var poDiff = new PersonopplysningGrunnlagDiff(aktørId, grunnlag1, grunnlag2);
        var påSkjæringstidpunkt = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt);
        var fraSkjæringstidpunkt = DatoIntervallEntitet.fraOgMed(skjæringstidspunkt);
        var forelderDødEndret = poDiff.erForeldreDødsdatoEndret();
        var personstatusEndret = poDiff.erPersonstatusEndretForSøkerPeriode(fraSkjæringstidpunkt);
        var personstatusUnntattDødEndret = personstatusEndret && !forelderDødEndret;

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
                    FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.DEKNINGSGRAD, "barnets dødsdato", grunnlag1.getId(), grunnlag2.getId());
                    startpunkter.add(StartpunktType.DEKNINGSGRAD);
                } else {
                    FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, "barnets dødsdato", grunnlag1.getId(), grunnlag2.getId());
                    startpunkter.add(StartpunktType.UTTAKSVILKÅR);
                }
            } else {
                FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.BEREGNING, "barnets dødsdato", grunnlag1.getId(), grunnlag2.getId());
                startpunkter.add(StartpunktType.BEREGNING);
            }
        }
        if (personstatusUnntattDødEndret) {
            leggTilBasertPåSTP(grunnlag1.getId(), grunnlag2.getId(), startpunkter, poDiff.erPersonstatusEndretForSøkerPeriode(påSkjæringstidpunkt), "personstatus");
        }
        if (poDiff.erAdresserEndretIPeriode(fraSkjæringstidpunkt)) {
            leggTilBasertPåSTP(grunnlag1.getId(), grunnlag2.getId(), startpunkter, poDiff.erAdresseLandEndretForSøkerPeriode(påSkjæringstidpunkt), "adresse");
        }
        if (poDiff.erRegionEndretForSøkerPeriode(fraSkjæringstidpunkt, skjæringstidspunkt)) {
            var aktivtGrunnlag = personopplysningRepository.hentPersonopplysninger(ref.behandlingId());
            var endretPåSTP = poDiff.erRegionEndretForSøkerPeriode(påSkjæringstidpunkt, skjæringstidspunkt) && !poDiff.harRegionNorden(påSkjæringstidpunkt, aktivtGrunnlag, skjæringstidspunkt);
            leggTilBasertPåSTP(grunnlag1.getId(), grunnlag2.getId(), startpunkter, endretPåSTP, "region");
        }
        if (barnBorteEndringIdentifiserer.erEndret(ref)) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.SØKERS_RELASJON_TIL_BARNET, "barn fjernet fra PDL", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.SØKERS_RELASJON_TIL_BARNET);
        }
        if(poDiff.erRelasjonerEndret()) {
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

    private void leggTilBasertPåSTP(Long g1Id, Long g2Id, List<StartpunktType> startpunkter, boolean endretFørStp, String loggMelding) {
        if (endretFørStp) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP, loggMelding, g1Id, g2Id);
            startpunkter.add(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP);
        } else {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, loggMelding, g1Id, g2Id);
            startpunkter.add(StartpunktType.UTTAKSVILKÅR);
        }
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

}
