package no.nav.foreldrepenger.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class OppgittOpptjeningBuilder {

    private final OppgittOpptjening kladd;

    private OppgittOpptjeningBuilder(OppgittOpptjening kladd) {
        this.kladd = kladd;
    }

    public static OppgittOpptjeningBuilder oppdater(Optional<OppgittOpptjening> kladd) {
        return kladd.map(oppgittOpptjening -> new OppgittOpptjeningBuilder(new OppgittOpptjening(oppgittOpptjening, UUID.randomUUID())))
            .orElseGet(() -> new OppgittOpptjeningBuilder(new OppgittOpptjening(UUID.randomUUID())));
    }

    public static OppgittOpptjeningBuilder ny() {
        return new OppgittOpptjeningBuilder(new OppgittOpptjening(UUID.randomUUID()));
    }

    public static OppgittOpptjeningBuilder ny(UUID eksternReferanse, LocalDateTime opprettetTidspunktOriginalt) {
        return new OppgittOpptjeningBuilder(new OppgittOpptjening(eksternReferanse, opprettetTidspunktOriginalt));
    }

    public static OppgittOpptjeningBuilder ny(UUID eksternReferanse, OffsetDateTime opprettetTidspunktOriginalt) {
        return new OppgittOpptjeningBuilder(
                new OppgittOpptjening(eksternReferanse, opprettetTidspunktOriginalt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()));
    }

    public OppgittOpptjeningBuilder leggTilAnnenAktivitet(OppgittAnnenAktivitet annenAktivitet) {
        this.kladd.leggTilAnnenAktivitet(annenAktivitet);
        return this;
    }

    public OppgittOpptjeningBuilder leggTilFrilansOpplysninger(OppgittFrilans frilans) {
        this.kladd.leggTilFrilans(frilans);
        return this;
    }

    public OppgittOpptjeningBuilder leggTilEgenNæring(List<EgenNæringBuilder> builders) {
        builders.forEach(builder -> this.kladd.leggTilEgenNæring(builder.build()));
        return this;
    }

    public OppgittOpptjeningBuilder leggTilEgenNæring(OppgittEgenNæring egenNæring) {
        this.kladd.leggTilEgenNæring(egenNæring);
        return this;
    }

    /**
     * Hvis det allerede finnes et objekt i liste egenNæring med matchende orgnr som nyEgenNæring, blir dette erastattet av nyEgenNæring.
     * Ellers blir nyEgenNæring lagt til
     * @param nyEgenNæring
     */
    public OppgittOpptjeningBuilder leggTilEllerErstattEgenNæring(OppgittEgenNæring nyEgenNæring) {
        this.kladd.leggTilEllerErstattEgenNæring(nyEgenNæring);
        return this;
    }

    public OppgittOpptjeningBuilder leggTilEllerErstattEgenNæringFjernAndreOrgnummer(OppgittEgenNæring nyEgenNæring) {
        this.kladd.leggTilEllerErstattEgenNæringFjernAndreOrgnummer(nyEgenNæring);
        return this;
    }

    public OppgittOpptjeningBuilder leggTilOppgittArbeidsforhold(OppgittArbeidsforhold oppgittArbeidsforhold) {
        this.kladd.leggTilOppgittArbeidsforhold(oppgittArbeidsforhold);
        return this;
    }

    public OppgittOpptjening build() {
        return kladd;
    }

    public static class EgenNæringBuilder {
        private final OppgittEgenNæring kladd;

        private EgenNæringBuilder(OppgittEgenNæring entitet) {
            this.kladd = entitet;
        }

        public static EgenNæringBuilder ny() {
            return new EgenNæringBuilder(new OppgittEgenNæring());
        }

        public EgenNæringBuilder medPeriode(DatoIntervallEntitet periode) {
            this.kladd.setPeriode(periode);
            return this;
        }

        public EgenNæringBuilder medVirksomhet(String orgNr) {
            return medVirksomhet(new OrgNummer(orgNr));
        }

        public EgenNæringBuilder medVirksomhetType(VirksomhetType type) {
            this.kladd.setVirksomhetType(type);
            return this;
        }

        public EgenNæringBuilder medRegnskapsførerNavn(String navn) {
            this.kladd.setRegnskapsførerNavn(navn);
            return this;
        }

        public EgenNæringBuilder medRegnskapsførerTlf(String tlf) {
            this.kladd.setRegnskapsførerTlf(tlf);
            return this;
        }

        public EgenNæringBuilder medEndringDato(LocalDate dato) {
            this.kladd.setEndringDato(dato);
            return this;
        }

        public EgenNæringBuilder medBegrunnelse(String begrunnelse) {
            this.kladd.setBegrunnelse(begrunnelse);
            return this;
        }

        public EgenNæringBuilder medNyoppstartet(boolean nyoppstartet) {
            this.kladd.setNyoppstartet(nyoppstartet);
            return this;
        }

        public EgenNæringBuilder medVarigEndring(boolean varigEndring) {
            this.kladd.setVarigEndring(varigEndring);
            return this;
        }

        public EgenNæringBuilder medNærRelasjon(boolean nærRelasjon) {
            this.kladd.setNærRelasjon(nærRelasjon);
            return this;
        }

        public EgenNæringBuilder medBruttoInntekt(BigDecimal bruttoInntekt) {
            this.kladd.setBruttoInntekt(bruttoInntekt);
            return this;
        }

        public EgenNæringBuilder medUtenlandskVirksomhet(OppgittUtenlandskVirksomhet utenlandskVirksomhet) {
            this.kladd.setUtenlandskVirksomhet(utenlandskVirksomhet);
            return this;
        }

        public OppgittEgenNæring build() {
            return kladd;
        }

        public EgenNæringBuilder medNyIArbeidslivet(boolean nyIArbeidslivet) {
            this.kladd.setNyIArbeidslivet(nyIArbeidslivet);
            return this;

        }

        public EgenNæringBuilder medVirksomhet(OrgNummer orgNr) {
            this.kladd.setVirksomhetOrgnr(orgNr);
            return this;
        }
    }

    public static class OppgittArbeidsforholdBuilder {
        private OppgittArbeidsforhold kladd;

        private OppgittArbeidsforholdBuilder(OppgittArbeidsforhold entitet) {
            this.kladd = entitet;
        }

        public static OppgittArbeidsforholdBuilder ny() {
            return new OppgittArbeidsforholdBuilder(new OppgittArbeidsforhold());
        }

        public OppgittArbeidsforholdBuilder medPeriode(DatoIntervallEntitet periode) {
            this.kladd.setPeriode(periode);
            return this;
        }

        public OppgittArbeidsforholdBuilder medErUtenlandskInntekt(Boolean erUtenlandskInntekt) {
            this.kladd.setErUtenlandskInntekt(erUtenlandskInntekt);
            return this;
        }

        public OppgittArbeidsforholdBuilder medArbeidType(ArbeidType arbeidType) {
            this.kladd.setArbeidType(arbeidType);
            return this;
        }

        public OppgittArbeidsforholdBuilder medUtenlandskVirksomhet(OppgittUtenlandskVirksomhet utenlandskVirksomhet) {
            this.kladd.setUtenlandskVirksomhet(utenlandskVirksomhet);
            return this;
        }

        public OppgittArbeidsforhold build() {
            return kladd;
        }
    }

    public static class OppgittFrilansBuilder {
        private OppgittFrilans kladd;

        private OppgittFrilansBuilder(OppgittFrilans entitet) {
            this.kladd = entitet;
        }

        public static OppgittFrilansBuilder ny() {
            return new OppgittFrilansBuilder(new OppgittFrilans());
        }

        public OppgittFrilansBuilder medErNyoppstartet(boolean erNyoppstartet) {
            this.kladd.setErNyoppstartet(erNyoppstartet);
            return this;
        }

        public OppgittFrilansBuilder medHarNærRelasjon(boolean harNærRelasjon) {
            this.kladd.setHarNærRelasjon(harNærRelasjon);
            return this;
        }

        public OppgittFrilansBuilder medHarInntektFraFosterhjem(boolean harInntektFraFosterhjem) {
            this.kladd.setHarInntektFraFosterhjem(harInntektFraFosterhjem);
            return this;
        }

        public OppgittFrilansBuilder leggTilFrilansoppdrag(OppgittFrilansoppdrag oppgittFrilansoppdrag) {
            this.kladd.leggTilFrilansoppdrag(oppgittFrilansoppdrag);
            return this;
        }

        public OppgittFrilans build() {
            return kladd;
        }
    }

}
