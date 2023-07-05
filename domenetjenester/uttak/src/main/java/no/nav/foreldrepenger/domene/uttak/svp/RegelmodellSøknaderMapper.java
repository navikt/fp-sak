package no.nav.foreldrepenger.domene.uttak.svp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.svangerskapspenger.domene.felles.AktivitetType;
import no.nav.svangerskapspenger.domene.felles.Arbeidsforhold;
import no.nav.svangerskapspenger.domene.søknad.DelvisTilrettelegging;
import no.nav.svangerskapspenger.domene.søknad.FullTilrettelegging;
import no.nav.svangerskapspenger.domene.søknad.IngenTilrettelegging;
import no.nav.svangerskapspenger.domene.søknad.Søknad;
import no.nav.svangerskapspenger.domene.søknad.Tilrettelegging;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RegelmodellSøknaderMapper {

    public List<Søknad> hentSøknader(UttakInput input) {
        SvangerskapspengerGrunnlag svpInput = input.getYtelsespesifiktGrunnlag();
        var termindato = finnTermindato(svpInput);
        return svpInput.getGrunnlagEntitet()
            .map(gr -> lagSøknader(input, gr, termindato))
            .orElse(List.of());
    }

    private LocalDate finnTermindato(SvangerskapspengerGrunnlag svpGrunnlag) {
        return svpGrunnlag.getFamilieHendelse().getTermindato()
            .orElseThrow(() -> new IllegalStateException("Det skal alltid være termindato på svangerskapspenger søknad."));
    }

    private List<Søknad> lagSøknader(UttakInput input, SvpGrunnlagEntitet svpGrunnlag, LocalDate termindato) {
        return svpGrunnlag.hentTilretteleggingerSomSkalBrukes()
            .stream()
            .map(tilrettelegging -> lagSøknad(input, tilrettelegging, termindato))
            .toList();
    }

    private Søknad lagSøknad(UttakInput input, SvpTilretteleggingEntitet tilrettelegging, LocalDate termindato) {
        var tilrettelegginger = tilrettelegging.getTilretteleggingFOMListe().stream().map(this::mapTilrettelegging).toList();
        var aktivitetType = mapTilAktivitetType(tilrettelegging.getArbeidType());
        var arbeidsforhold = lagArbeidsforhold(tilrettelegging.getArbeidsgiver(), tilrettelegging.getInternArbeidsforholdRef(), aktivitetType);
        var stillingsprosent = finnStillingsprosent(input, tilrettelegging, arbeidsforhold);
        return new Søknad(arbeidsforhold, stillingsprosent, termindato, tilrettelegging.getBehovForTilretteleggingFom(), tilrettelegginger);
    }

    private Tilrettelegging mapTilrettelegging(TilretteleggingFOM tl) {
        if (tl.getType().equals(TilretteleggingType.HEL_TILRETTELEGGING)) {
            return new FullTilrettelegging(tl.getFomDato());
        }
        if (tl.getType().equals(TilretteleggingType.DELVIS_TILRETTELEGGING)) {
            return new DelvisTilrettelegging(tl.getFomDato(), tl.getStillingsprosent());
        }
        if (tl.getType().equals(TilretteleggingType.INGEN_TILRETTELEGGING)) {
            return new IngenTilrettelegging(tl.getFomDato());
        }
        throw new IllegalStateException("Mangler gyldig TilretteleggingType");
    }

    public static AktivitetType mapTilAktivitetType(ArbeidType arbeidType) {
        if (ArbeidType.ORDINÆRT_ARBEIDSFORHOLD.equals(arbeidType)) {
            return AktivitetType.ARBEID;
        }
        if (ArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE.equals(arbeidType)) {
            return AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;
        }
        if (ArbeidType.FRILANSER.equals(arbeidType)) {
            return AktivitetType.FRILANS;
        }
        return AktivitetType.ANNET;
    }

    private BigDecimal finnStillingsprosent(UttakInput input, SvpTilretteleggingEntitet svpTilrettelegging, Arbeidsforhold arbeidsforhold) {
        if (svpTilrettelegging.getArbeidType().equals(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD) && svpTilrettelegging.getArbeidsgiver().isPresent()) {
            var arbeidsgiver = arbeidsforhold.getArbeidsgiverVirksomhetId() == null ? null : Arbeidsgiver.virksomhet(arbeidsforhold.getArbeidsgiverVirksomhetId());
            return input.getYrkesaktiviteter().finnStillingsprosentOrdinærtArbeid(
                arbeidsgiver,
                InternArbeidsforholdRef.ref(arbeidsforhold.getArbeidsforholdId().orElse(null)),
                svpTilrettelegging.getBehovForTilretteleggingFom()
            );
        }
        return BigDecimal.valueOf(100L); //Ellers 100% stilling
    }

    public static Arbeidsforhold lagArbeidsforhold(Optional<Arbeidsgiver> arbeidsforhold,
                                             Optional<InternArbeidsforholdRef> internArbeidsforholdRef,
                                             AktivitetType aktivitetType) {
        if (arbeidsforhold.isEmpty()) {
            return Arbeidsforhold.annet(aktivitetType);
        }
        var arbeidsgiver = arbeidsforhold.get();
        if (internArbeidsforholdRef.isEmpty()) {
            if (arbeidsgiver.getErVirksomhet()) {
                return Arbeidsforhold.virksomhet(aktivitetType, arbeidsgiver.getOrgnr(), null);
            }
            return Arbeidsforhold.aktør(aktivitetType, arbeidsgiver.getAktørId().getId(), null);
        }
        if (arbeidsgiver.getErVirksomhet()) {
            return Arbeidsforhold.virksomhet(aktivitetType, arbeidsgiver.getOrgnr(), internArbeidsforholdRef.get().getReferanse());
        }
        return Arbeidsforhold.aktør(aktivitetType, arbeidsgiver.getAktørId().getId(), internArbeidsforholdRef.get().getReferanse());
    }

}
