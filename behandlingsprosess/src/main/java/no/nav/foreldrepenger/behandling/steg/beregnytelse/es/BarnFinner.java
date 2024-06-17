package no.nav.foreldrepenger.behandling.steg.beregnytelse.es;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.vedtak.exception.FunksjonellException;

class BarnFinner {

    private final FamilieHendelseRepository familieGrunnlagRepository;

    BarnFinner(FamilieHendelseRepository familieGrunnlagRepository) {
        this.familieGrunnlagRepository = familieGrunnlagRepository;
    }

    int finnAntallBarn(Long behandlingId, int maksStønadsalderAdopsjon) {

        var grunnlag = familieGrunnlagRepository.hentAggregat(behandlingId);
        var barnSøktFor = getBarnInfoer(grunnlag);
        return finnAntallBarn(maksStønadsalderAdopsjon, grunnlag, barnSøktFor);
    }

    private int finnAntallBarn(int maksStønadsalderAdopsjon, final FamilieHendelseGrunnlagEntitet grunnlag, List<BarnInfo> barnSøktFor) {
        var barnKvalifisertForYtelse = Objects.equals(FamilieHendelseType.ADOPSJON,
            grunnlag.getGjeldendeVersjon().getType()) ? barnKvalifisertForAdopsjon(maksStønadsalderAdopsjon, grunnlag, barnSøktFor) : barnSøktFor;

        if (barnKvalifisertForYtelse.isEmpty()) {
            throw new FunksjonellException("FP-110705", "Kan ikke beregne ytelse. Finner ikke barn som har rett til ytelse i behandlingsgrunnlaget.",
                "Sjekk avklarte fakta i behandlingen. Oppdater fakta slik at det finnes barn ");
        }
        return barnKvalifisertForYtelse.size();
    }

    private List<BarnInfo> barnKvalifisertForAdopsjon(int maksStønadsalderAdopsjon,
                                                      final FamilieHendelseGrunnlagEntitet grunnlag,
                                                      List<BarnInfo> barnSøktFor) {
        var gjeldendeAdopsjon = grunnlag.getGjeldendeAdopsjon();
        if (gjeldendeAdopsjon.isEmpty()) {
            // skal aldri kunne skje, men logikken for å sjekke ifPresent er basert på
            // negativ testing hvilket kan være ustabilt.
            // legger derfor på her
            throw new IllegalStateException("Mangler grunnlag#getGjeldendeAdopsjon i " + grunnlag);
        }

        var adopsjon = gjeldendeAdopsjon.get();
        var eldsteFristForOmsorgsovertakelse = adopsjon.getOmsorgsovertakelseDato().minusYears(maksStønadsalderAdopsjon);

        return barnSøktFor.stream().filter(barn -> {
            var fødselsdato = barn.getFødselsdato();
            return fødselsdato.isAfter(eldsteFristForOmsorgsovertakelse);
        }).toList();
    }

    private List<BarnInfo> getBarnInfoer(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        var type = familieHendelseGrunnlag.getGjeldendeVersjon().getType();
        if (Objects.equals(FamilieHendelseType.FØDSEL, type) || Objects.equals(FamilieHendelseType.TERMIN, type)) {
            return fødselsvilkårTilBarnInfoer(familieHendelseGrunnlag);
        }
        if (Objects.equals(FamilieHendelseType.ADOPSJON, type)) {
            return adopsjonsvilkårTilBarnInfoer(familieHendelseGrunnlag);
        }
        if (Objects.equals(FamilieHendelseType.OMSORG, type)) {
            return adopsjonsvilkårTilBarnInfoer(familieHendelseGrunnlag);
        }
        return Collections.emptyList();
    }

    private List<BarnInfo> fødselsvilkårTilBarnInfoer(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        var gjeldendeVersjon = familieHendelseGrunnlag.getGjeldendeVersjon();
        var gjeldendeBarn = familieHendelseGrunnlag.getGjeldendeBarna();

        if (FamilieHendelseType.FØDSEL.equals(gjeldendeVersjon.getType()) && !gjeldendeBarn.isEmpty()) {
            return gjeldendeBarn.stream().map(it -> new BarnInfo(it.getBarnNummer(), it.getFødselsdato(), null)).toList();
        }
        var gjeldendeTerminbekreftelse = familieHendelseGrunnlag.getGjeldendeTerminbekreftelse();
        if (gjeldendeTerminbekreftelse.isPresent()) {
            var terminbekreftelse = gjeldendeTerminbekreftelse.get();
            var antallBarn = gjeldendeVersjon.getAntallBarn();
            List<BarnInfo> barnInfoer = new ArrayList<>();
            for (var i = 0; i < antallBarn; i++) {
                barnInfoer.add(new BarnInfo(i, terminbekreftelse.getTermindato(), null));
            }
            return barnInfoer;
        }
        return Collections.emptyList();
    }

    private List<BarnInfo> adopsjonsvilkårTilBarnInfoer(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        return familieHendelseGrunnlag.getGjeldendeBarna()
            .stream()
            .map(adopsjonBarn -> new BarnInfo(adopsjonBarn.getBarnNummer(), adopsjonBarn.getFødselsdato(), null))
            .toList();
    }
}
