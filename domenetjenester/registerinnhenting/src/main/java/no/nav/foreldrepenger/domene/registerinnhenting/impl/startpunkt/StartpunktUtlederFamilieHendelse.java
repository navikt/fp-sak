package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FamilieHendelseDato;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.GrunnlagRef;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktUtleder;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@GrunnlagRef(FamilieHendelseGrunnlagEntitet.ENTITY_NAME)
class StartpunktUtlederFamilieHendelse implements StartpunktUtleder {

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;

    StartpunktUtlederFamilieHendelse() {
        // For CDI
    }

    @Inject
    StartpunktUtlederFamilieHendelse(SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                     FamilieHendelseTjeneste familieHendelseTjeneste,
                                     DekningsgradTjeneste dekningsgradTjeneste) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
    }

    @Override
    public StartpunktType utledStartpunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, Object id1, Object id2) {
        var grunnlag1 = (long) id1;
        var grunnlag2 = (long) id2;

        if (erSkjæringstidspunktEndret(ref, stp)) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT, "skjæringstidspunkt", grunnlag1, grunnlag2);
            return StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;
        }
        if (erAntallBekreftedeBarnEndret(grunnlag1, grunnlag2)) {
            FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.SØKERS_RELASJON_TIL_BARNET, "antall barn", grunnlag1, grunnlag2);
            return StartpunktType.SØKERS_RELASJON_TIL_BARNET;
        }
        if (erTilkommetRegisterDødfødselMedDekningsgrad80(ref, grunnlag1, grunnlag2)) {
            if (ref.fagsakYtelseType() == FagsakYtelseType.FORELDREPENGER) {
                FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.DEKNINGSGRAD, "antall barn", grunnlag1, grunnlag2);
                return StartpunktType.DEKNINGSGRAD;
            } else {
                FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.BEREGNING, "antall barn", grunnlag1, grunnlag2);
                return StartpunktType.BEREGNING;
            }
        }
        FellesStartpunktUtlederLogger.loggEndringSomFørteTilStartpunkt(this.getClass().getSimpleName(), StartpunktType.UTTAKSVILKÅR, "familiehendelse", grunnlag1, grunnlag2);
        return StartpunktType.UTTAKSVILKÅR;
    }

    private boolean erSkjæringstidspunktEndret(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var nySkjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
        var origSkjæringstidspunkt = ref.getOriginalBehandlingId()
            .map(origId -> skjæringstidspunktTjeneste.getSkjæringstidspunkter(origId).getUtledetSkjæringstidspunkt());

        var nyBekreftetFødselsdato = stp.getFamilieHendelseDato()
            .map(FamilieHendelseDato::familieHendelseDato).orElse(null);
        var origBekreftetFødselsdato = ref.getOriginalBehandlingId()
            .flatMap(origId -> familieHendelseTjeneste.hentAggregat(origId).getGjeldendeBekreftetVersjon())
            .flatMap(FamilieHendelseEntitet::getFødselsdato).orElse(null);

        /*
         * Logikk hovedsaklig knyttet til mor som har STP 3 uker før termin og tilfelle av fødsel før STP.
         * Deretter far/medmor som tar ut rundt fødsel (fom aug 2022) og har søkt om flyttbare perioder som skal justeres til fom fødsel
         */
        if (nyBekreftetFødselsdato != null) {
            if (origBekreftetFødselsdato == null || nyBekreftetFødselsdato.isBefore(origBekreftetFødselsdato)) {
                // Familiehendelse har blitt bekreftet etter original behandling, eller flyttet til tidligere dato
                if (nyBekreftetFødselsdato.isBefore(nySkjæringstidspunkt)) {
                    return true;
                }
            }
            if (!RelasjonsRolleType.MORA.equals(ref.relasjonRolle()) && stp.uttakSkalJusteresTilFødselsdato() && !nyBekreftetFødselsdato.equals(origBekreftetFødselsdato)) {
                var nyFørsteUttaksdato = VirkedagUtil.fomVirkedag(nyBekreftetFødselsdato);
                if (!nyFørsteUttaksdato.equals(nySkjæringstidspunkt) || origSkjæringstidspunkt.filter(nyFørsteUttaksdato::equals).isEmpty()) {
                    return true;
                }
            }
        }

        return origSkjæringstidspunkt.filter(nySkjæringstidspunkt::isBefore).isPresent();
    }

    private boolean erAntallBekreftedeBarnEndret(Long id1, Long id2) {
        var grunnlag1 = familieHendelseTjeneste.hentGrunnlagPåId(id1);
        var grunnlag2 = familieHendelseTjeneste.hentGrunnlagPåId(id2);
        var antallBarn1 = grunnlag1.getGjeldendeVersjon().getAntallBarn();
        var antallBarn2 = grunnlag2.getGjeldendeVersjon().getAntallBarn();

        return !antallBarn1.equals(antallBarn2);
    }

    private boolean erTilkommetRegisterDødfødselMedDekningsgrad80(BehandlingReferanse referanse, Long id1, Long id2) {
        var grunnlag1 = familieHendelseTjeneste.hentGrunnlagPåId(id1);
        var grunnlag2 = familieHendelseTjeneste.hentGrunnlagPåId(id2);
        var dødfødsel1 = grunnlag1.getGjeldendeBekreftetVersjon().filter(FamilieHendelseEntitet::getInnholderDøfødtBarn).isPresent();
        var dødfødsel2 = grunnlag2.getGjeldendeBekreftetVersjon().filter(FamilieHendelseEntitet::getInnholderDøfødtBarn).isPresent();

        return dødfødsel1 != dødfødsel2 && dekningsgradTjeneste.finnGjeldendeDekningsgradHvisEksisterer(referanse)
            .filter(Dekningsgrad._80::equals)
            .isPresent();

    }

}
