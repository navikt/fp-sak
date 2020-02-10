package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;
import no.nav.foreldrepenger.familiehendelse.dødsfall.BarnBorteEndringIdentifiserer;

@ApplicationScoped
@GrunnlagRef("PersonInformasjon")
class StartpunktUtlederPersonopplysning implements StartpunktUtleder {

    private PersonopplysningRepository personopplysningRepository;

    private BarnBorteEndringIdentifiserer barnBorteEndringIdentifiserer;

    StartpunktUtlederPersonopplysning() {
        // For CDI
    }

    @Inject
    StartpunktUtlederPersonopplysning(PersonopplysningRepository personopplysningRepository,
                                      BarnBorteEndringIdentifiserer barnBorteEndringIdentifiserer) {
        this.personopplysningRepository = personopplysningRepository;
        this.barnBorteEndringIdentifiserer = barnBorteEndringIdentifiserer;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2) {
        PersonopplysningGrunnlagEntitet grunnlag1 = personopplysningRepository.hentPersonopplysningerPåId((Long)grunnlagId1);
        PersonopplysningGrunnlagEntitet grunnlag2 = personopplysningRepository.hentPersonopplysningerPåId((Long)grunnlagId2);
        return utled(ref, grunnlag1, grunnlag2);
    }

    private StartpunktType utled(BehandlingReferanse ref, PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2) {

        return hentAlleStartpunktForPersonopplysninger(ref, grunnlag1, grunnlag2).stream()
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    // Finn endringer per aggregat under grunnlaget og map dem mot startpunkt. Dekker bruker og TPS-relaterte personer (barn, ekte). Bør spisses der det er behov.
    private List<StartpunktType> hentAlleStartpunktForPersonopplysninger(BehandlingReferanse ref, PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2) {
        final LocalDate skjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();
        var aktørId = ref.getAktørId();

        PersonopplysningGrunnlagDiff poDiff = new PersonopplysningGrunnlagDiff(aktørId, grunnlag1, grunnlag2);
        boolean forelderDødEndret = poDiff.erForeldreDødsdatoEndret();
        boolean personstatusEndret = poDiff.erPersonstatusEndretForSøkerFør(null);
        boolean personstatusUnntattDødEndret = personstatusUnntattDødEndret(personstatusEndret, forelderDødEndret);

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
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.BEREGNING, "barnets dødsdato", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.BEREGNING);
        }
        if (personstatusUnntattDødEndret) {
            leggTilBasertPåSTP(grunnlag1.getId(), grunnlag2.getId(), startpunkter, poDiff.erPersonstatusEndretForSøkerFør(skjæringstidspunkt), "personstatus");
        }
        if (poDiff.erAdresserEndretFør(null)) {
            leggTilBasertPåSTP(grunnlag1.getId(), grunnlag2.getId(), startpunkter, poDiff.erSøkersAdresseEndretFør(skjæringstidspunkt), "adresse");
        }
        if (poDiff.erRegionEndretForBruker()) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP, "region", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP);
        }
        if (barnBorteEndringIdentifiserer.erEndret(ref)) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.SØKERS_RELASJON_TIL_BARNET, "barn fjernet fra TPS", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.SØKERS_RELASJON_TIL_BARNET);
        }
        if(poDiff.erRelasjonerEndret()) {
            leggTilForRelasjoner(grunnlag1.getId(), grunnlag2.getId(), poDiff, startpunkter);
        }
        if (startpunkter.isEmpty()) {
            // Endringen som trigget utledning av startpunkt skal ikke styre startpunkt
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UDEFINERT, "personopplysning - andre endringer", grunnlag1.getId(), grunnlag2.getId());
            startpunkter.add(StartpunktType.UDEFINERT);
        }
        return startpunkter;
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
    }

    private boolean personstatusUnntattDødEndret(boolean personstatusEndret, boolean søkerErDødEndret) {
        return personstatusEndret && !søkerErDødEndret;
    }
}
