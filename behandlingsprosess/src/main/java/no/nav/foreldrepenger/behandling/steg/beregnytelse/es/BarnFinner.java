package no.nav.foreldrepenger.behandling.steg.beregnytelse.es;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.feil.LogLevel;

class BarnFinner {

    private final FamilieHendelseRepository familieGrunnlagRepository;

    BarnFinner(FamilieHendelseRepository familieGrunnlagRepository) {
        this.familieGrunnlagRepository = familieGrunnlagRepository;
    }

    int finnAntallBarn(Long behandlingId, int maksStønadsalderAdopsjon) {

        final FamilieHendelseGrunnlagEntitet grunnlag = familieGrunnlagRepository.hentAggregat(behandlingId);
        List<BarnInfo> barnSøktFor = getBarnInfoer(grunnlag);
        return finnAntallBarn(maksStønadsalderAdopsjon, grunnlag, barnSøktFor);
    }

    private int finnAntallBarn(int maksStønadsalderAdopsjon, final FamilieHendelseGrunnlagEntitet grunnlag,
            List<BarnInfo> barnSøktFor) {
        List<BarnInfo> barnKvalifisertForYtelse = Objects.equals(FamilieHendelseType.ADOPSJON, grunnlag.getGjeldendeVersjon().getType())
                ? barnKvalifisertForAdopsjon(maksStønadsalderAdopsjon, grunnlag, barnSøktFor)
                : barnSøktFor;

        if (barnKvalifisertForYtelse.isEmpty()) {
            throw new FunksjonellException("FP-110705",
                "Kan ikke beregne ytelse. Finner ikke barn som har rett til ytelse i behandlingsgrunnlaget.",
                //TODO palfi
                "Sjekk avklarte fakta i behandlingen. Oppdater fakta slik at det finnes barn ", LogLevel.WARN, null);
        }
        return barnKvalifisertForYtelse.size();
    }

    private List<BarnInfo> barnKvalifisertForAdopsjon(int maksStønadsalderAdopsjon, final FamilieHendelseGrunnlagEntitet grunnlag,
            List<BarnInfo> barnSøktFor) {
        Optional<AdopsjonEntitet> gjeldendeAdopsjon = grunnlag.getGjeldendeAdopsjon();
        if (gjeldendeAdopsjon.isEmpty()) {
            // skal aldri kunne skje, men logikken for å sjekke ifPresent er basert på
            // negativ testing hvilket kan være ustabilt.
            // legger derfor på her
            throw new IllegalStateException("Mangler grunnlag#getGjeldendeAdopsjon i " + grunnlag);
        }

        AdopsjonEntitet adopsjon = gjeldendeAdopsjon.get();
        LocalDate eldsteFristForOmsorgsovertakelse = adopsjon.getOmsorgsovertakelseDato().minusYears(maksStønadsalderAdopsjon);

        return barnSøktFor.stream()
                .filter(barn -> {
                    LocalDate fødselsdato = barn.getFødselsdato();
                    return fødselsdato.isAfter(eldsteFristForOmsorgsovertakelse);
                })
                .collect(Collectors.toList());
    }

    private List<BarnInfo> getBarnInfoer(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        final FamilieHendelseType type = familieHendelseGrunnlag.getGjeldendeVersjon().getType();
        if (Objects.equals(FamilieHendelseType.FØDSEL, type) || Objects.equals(FamilieHendelseType.TERMIN, type)) {
            return fødselsvilkårTilBarnInfoer(familieHendelseGrunnlag);
        } else if (Objects.equals(FamilieHendelseType.ADOPSJON, type)) {
            return adopsjonsvilkårTilBarnInfoer(familieHendelseGrunnlag);
        } else if (Objects.equals(FamilieHendelseType.OMSORG, type)) {
            return adopsjonsvilkårTilBarnInfoer(familieHendelseGrunnlag);
        } else {
            return Collections.emptyList();
        }
    }

    private List<BarnInfo> fødselsvilkårTilBarnInfoer(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        final FamilieHendelseEntitet gjeldendeVersjon = familieHendelseGrunnlag.getGjeldendeVersjon();
        final List<UidentifisertBarn> gjeldendeBarn = familieHendelseGrunnlag.getGjeldendeBarna();

        if (FamilieHendelseType.FØDSEL.equals(gjeldendeVersjon.getType()) && !gjeldendeBarn.isEmpty()) {
            return gjeldendeBarn.stream()
                    .map(it -> new BarnInfo(it.getBarnNummer(), it.getFødselsdato(), null))
                    .collect(Collectors.toList());
        } else {
            Optional<TerminbekreftelseEntitet> gjeldendeTerminbekreftelse = familieHendelseGrunnlag.getGjeldendeTerminbekreftelse();
            if (gjeldendeTerminbekreftelse.isPresent()) {
                TerminbekreftelseEntitet terminbekreftelse = gjeldendeTerminbekreftelse.get();
                Integer antallBarn = gjeldendeVersjon.getAntallBarn();
                List<BarnInfo> barnInfoer = new ArrayList<>();
                for (int i = 0; i < antallBarn; i++) {
                    barnInfoer.add(new BarnInfo(i, terminbekreftelse.getTermindato(), null));
                }
                return barnInfoer;
            } else {
                return Collections.emptyList();
            }
        }
    }

    private List<BarnInfo> adopsjonsvilkårTilBarnInfoer(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        return familieHendelseGrunnlag.getGjeldendeBarna().stream()
                .map(adopsjonBarn -> new BarnInfo(adopsjonBarn.getBarnNummer(), adopsjonBarn.getFødselsdato(), null))
                .collect(Collectors.toList());
    }
}
