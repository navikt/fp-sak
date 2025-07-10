package no.nav.foreldrepenger.domene.abakus.mapping;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.abakus.iaygrunnlag.Aktør;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.PersonIdent;
import no.nav.abakus.iaygrunnlag.inntekt.v1.InntekterDto;
import no.nav.abakus.iaygrunnlag.inntekt.v1.UtbetalingDto;
import no.nav.abakus.iaygrunnlag.inntekt.v1.UtbetalingsPostDto;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;

class MapAktørInntekt {

    private MapAktørInntekt() {
        // Skjul konstruktør
    }

    static class MapFraDto {

        private final InntektArbeidYtelseAggregatBuilder aggregatBuilder;

        MapFraDto(InntektArbeidYtelseAggregatBuilder aggregatBuilder) {
            this.aggregatBuilder = aggregatBuilder;
        }

        List<AktørInntektBuilder> map(Collection<InntekterDto> dtos) {
            if (dtos == null || dtos.isEmpty()) {
                return Collections.emptyList();
            }

            return dtos.stream().map(idto -> {
                var builder = aggregatBuilder.getAktørInntektBuilder(tilAktørId(idto.getPerson()));
                idto.getUtbetalinger().forEach(utbetalingDto -> builder.leggTilInntekt(mapUtbetaling(utbetalingDto)));
                return builder;
            }).toList();
        }

        /**
         * Returnerer person sin aktørId. Denne trenger ikke være samme som søkers
         * aktørid men kan f.eks. være annen part i en sak.
         */
        private AktørId tilAktørId(PersonIdent person) {
            if (!(person instanceof AktørIdPersonident)) {
                throw new IllegalArgumentException("Støtter kun " + AktørIdPersonident.class.getSimpleName() + " her");
            }
            return new AktørId(person.getIdent());
        }

        private InntektBuilder mapUtbetaling(UtbetalingDto dto) {
            var inntektBuilder = InntektBuilder.oppdatere(Optional.empty())
                    .medArbeidsgiver(mapArbeidsgiver(dto.getUtbetaler()))
                    .medInntektsKilde(KodeverkMapper.mapInntektsKildeFraDto(dto.getKilde()));
            dto.getPoster()
                    .forEach(post -> inntektBuilder.leggTilInntektspost(mapInntektspost(post)));
            return inntektBuilder;
        }

        private InntektspostBuilder mapInntektspost(UtbetalingsPostDto post) {
            return InntektspostBuilder.ny()
                    .medBeløp(post.getBeløp())
                    .medInntektspostType(KodeverkMapper.mapInntektspostTypeFraDto(post.getInntektspostType()))
                    .medPeriode(post.getPeriode().getFom(), post.getPeriode().getTom())
                    .medSkatteOgAvgiftsregelType(KodeverkMapper.mapSkatteOgAvgiftsregelFraDto(post.getSkattAvgiftType()))
                    .medInntektYtelse(KodeverkMapper.mapInntektYtelseTypeTilGrunnlag(post.getInntektYtelseType()));
        }

        private Arbeidsgiver mapArbeidsgiver(Aktør arbeidsgiver) {
            if (arbeidsgiver == null) {
                return null;
            }
            if (arbeidsgiver.getErOrganisasjon()) {
                return Arbeidsgiver.virksomhet(new OrgNummer(arbeidsgiver.getIdent()));
            }
            return Arbeidsgiver.person(new AktørId(arbeidsgiver.getIdent()));
        }

    }

}
