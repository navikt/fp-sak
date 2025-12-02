package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;


public class SvpArbeidsforholdDto {

    @NotNull
    @Min(0L)
    @Max(Long.MAX_VALUE)
    private Long tilretteleggingId;
    @NotNull private LocalDate tilretteleggingBehovFom;
    @NotNull private List<@Valid SvpTilretteleggingDatoDto> tilretteleggingDatoer = new ArrayList<>();
    @ValidKodeverk
    private UttakArbeidType uttakArbeidType;
    @Pattern(regexp = InputValideringRegex.ARBEIDSGIVER)
    private String arbeidsgiverReferanse;
    private UUID internArbeidsforholdReferanse;
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String eksternArbeidsforholdReferanse;
    @NotNull private boolean skalBrukes = true;
    @NotNull private boolean kanTilrettelegges = true;
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal stillingsprosentStartTilrettelegging;
    @NotNull private List<@Valid VelferdspermisjonDto> velferdspermisjoner = new ArrayList<>();
    @NotNull private List<@Valid SvpAvklartOppholdPeriodeDto> avklarteOppholdPerioder = new ArrayList<>();

    @Size(max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String begrunnelse;

    public LocalDate getTilretteleggingBehovFom() {
        return tilretteleggingBehovFom;
    }

    public void setTilretteleggingBehovFom(LocalDate tilretteleggingBehovFom) {
        this.tilretteleggingBehovFom = tilretteleggingBehovFom;
    }

    public List<SvpTilretteleggingDatoDto> getTilretteleggingDatoer() {
        return tilretteleggingDatoer;
    }

    public void setTilretteleggingDatoer(List<SvpTilretteleggingDatoDto> tilretteleggingDatoer) {
        this.tilretteleggingDatoer = tilretteleggingDatoer;
    }

    public Long getTilretteleggingId() {
        return tilretteleggingId;
    }

    public void setTilretteleggingId(Long tilretteleggingId) {
        this.tilretteleggingId = tilretteleggingId;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }


    public void setInternArbeidsforholdReferanse(UUID internArbeidsforholdRef) {
        this.internArbeidsforholdReferanse = internArbeidsforholdRef;
    }

    public UUID getInternArbeidsforholdReferanse() {
        return internArbeidsforholdReferanse;
    }

    public boolean getSkalBrukes() {
        return skalBrukes;
    }

    public List<SvpAvklartOppholdPeriodeDto> getAvklarteOppholdPerioder() {
        return avklarteOppholdPerioder;
    }

    public void setSkalBrukes(boolean skalBrukes) {
        this.skalBrukes = skalBrukes;
    }

    public void setKanTilrettelegges(boolean kanTilrettelegges) {
        this.kanTilrettelegges = kanTilrettelegges;
    }

    public boolean isKanTilrettelegges() {
        return kanTilrettelegges;
    }

    public String getEksternArbeidsforholdReferanse() {
        return eksternArbeidsforholdReferanse;
    }

    public void setEksternArbeidsforholdReferanse(String eksternArbeidsforholdReferanse) {
        this.eksternArbeidsforholdReferanse = eksternArbeidsforholdReferanse;
    }

    public List<VelferdspermisjonDto> getVelferdspermisjoner() {
        return velferdspermisjoner;
    }

    public void setVelferdspermisjoner(List<VelferdspermisjonDto> velferdspermisjoner) {
        this.velferdspermisjoner = velferdspermisjoner;
    }

    public void setAvklarteOppholdPerioder(List<SvpAvklartOppholdPeriodeDto> avklarteOppholdPerioder) {
        this.avklarteOppholdPerioder = avklarteOppholdPerioder;
    }
    public void leggTilAvklarteOppholdPerioder(List<SvpAvklartOppholdPeriodeDto> avklarteOppholdPerioder) {
        avklarteOppholdPerioder.forEach(oppholdPeriode -> this.avklarteOppholdPerioder.add(oppholdPeriode));
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public void setArbeidsgiverReferanse(String arbeidsgiverReferanse) {
        this.arbeidsgiverReferanse = arbeidsgiverReferanse;
    }

    public UttakArbeidType getUttakArbeidType() {
        return uttakArbeidType;
    }

    public void setUttakArbeidType(UttakArbeidType uttakArbeidType) {
        this.uttakArbeidType = uttakArbeidType;
    }

    public BigDecimal getStillingsprosentStartTilrettelegging() {
        return stillingsprosentStartTilrettelegging;
    }

    public void setStillingsprosentStartTilrettelegging(BigDecimal stillingsprosentStartTilrettelegging) {
        this.stillingsprosentStartTilrettelegging = stillingsprosentStartTilrettelegging;
    }
}
