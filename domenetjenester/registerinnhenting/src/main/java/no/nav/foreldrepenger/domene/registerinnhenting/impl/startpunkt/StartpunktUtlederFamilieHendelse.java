package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@GrunnlagRef("FamilieHendelseGrunnlag")
class StartpunktUtlederFamilieHendelse implements StartpunktUtleder {

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    StartpunktUtlederFamilieHendelse() {
        // For CDI
    }

    @Inject
    StartpunktUtlederFamilieHendelse(SkjæringstidspunktTjeneste skjæringstidspunktTjeneste, FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    @Override
    public boolean erBehovForStartpunktUtledning(EndringsresultatDiff diff) {
        return true;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Object id1, Object id2) {
        long grunnlag1 = (long) id1;
        long grunnlag2 = (long) id2;

        FamilieHendelseGrunnlagEntitet grunnlagForBehandling = familieHendelseTjeneste.hentAggregat(ref.getBehandlingId());
        if (erSkjæringstidspunktEndret(ref, grunnlagForBehandling)) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT, "skjæringstidspunkt", grunnlag1, grunnlag2);
            return StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;
        }
        if (erAntallBekreftedeBarnEndret(grunnlag1, grunnlag2)) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.SØKERS_RELASJON_TIL_BARNET, "antall barn", grunnlag1, grunnlag2);
            return StartpunktType.SØKERS_RELASJON_TIL_BARNET;
        }
        if (skalSjekkeForManglendeFødsel(grunnlagForBehandling)) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.SØKERS_RELASJON_TIL_BARNET, "manglende fødsel", grunnlag1, grunnlag2);
            return StartpunktType.SØKERS_RELASJON_TIL_BARNET;
        }

        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, "familiehendelse", grunnlag1, grunnlag2);
        return StartpunktType.UTTAKSVILKÅR;
    }

    private boolean erSkjæringstidspunktEndret(BehandlingReferanse ref, FamilieHendelseGrunnlagEntitet grunnlagForBehandling) {
        LocalDate nySkjæringstidspunkt = ref.getUtledetSkjæringstidspunkt();
        Optional<LocalDate> origSkjæringstidspunkt = ref.getOriginalBehandlingId()
            .map(origId -> skjæringstidspunktTjeneste.getSkjæringstidspunkter(origId).getUtledetSkjæringstidspunkt());

        Optional<LocalDate> nyBekreftetFødselsdato = grunnlagForBehandling.getGjeldendeBekreftetVersjon()
            .flatMap(FamilieHendelseEntitet::getFødselsdato);
        Optional<LocalDate> origBekreftetFødselsdato = ref.getOriginalBehandlingId()
            .flatMap(origId -> familieHendelseTjeneste.hentAggregat(origId).getGjeldendeBekreftetVersjon())
            .flatMap(FamilieHendelseEntitet::getFødselsdato);

        if (nyBekreftetFødselsdato.isPresent()) {
            if (!origBekreftetFødselsdato.isPresent()
                || nyBekreftetFødselsdato.get().isBefore(origBekreftetFødselsdato.get())) {

                // Familiehendelse har blitt bekreftet etter original behandling, eller flyttet til tidligere dato
                if (nyBekreftetFødselsdato.get().isBefore(nySkjæringstidspunkt)) {
                    return true;
                }
            }
        }

        if (origSkjæringstidspunkt.isPresent()) {
            return nySkjæringstidspunkt.isBefore(origSkjæringstidspunkt.get());
        }
        return false;
    }

    private boolean erAntallBekreftedeBarnEndret(Long id1, Long id2) {
        FamilieHendelseGrunnlagEntitet grunnlag1 = familieHendelseTjeneste.hentFamilieHendelserPåGrunnlagId(id1);
        FamilieHendelseGrunnlagEntitet grunnlag2 = familieHendelseTjeneste.hentFamilieHendelserPåGrunnlagId(id2);
        Integer antallBarn1 = grunnlag1.getGjeldendeVersjon().getAntallBarn();
        Integer antallBarn2 = grunnlag2.getGjeldendeVersjon().getAntallBarn();

        return !antallBarn1.equals(antallBarn2);
    }

    private boolean skalSjekkeForManglendeFødsel(FamilieHendelseGrunnlagEntitet grunnlagForBehandling) {
        return familieHendelseTjeneste.getManglerFødselsRegistreringFristUtløpt(grunnlagForBehandling);
    }
}
