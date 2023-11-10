package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;

public class Oppdragslinje150Dto extends SporingDto {

    private String vedtakId;
    private String utbetalesTilId;
    private Long delytelseId;
    private Long refDelytelseId;
    private Long refFagsystemId;
    private String kodeEndringLinje;
    private String kodeStatusLinje;
    private LocalDate datoStatusFom;
    private LocalDate datoVedtakFom;
    private LocalDate datoVedtakTom;
    private KodeKlassifik kodeKlassifik;
    private long sats;
    private String typeSats;
    private Integer utbetalingsgrad;
    private RefusjonInfo156Dto refusjonInfo156;

    public Oppdragslinje150Dto(Oppdragslinje150 entitet) {
        super(entitet, 0L, entitet.getId());
    }

    public String getVedtakId() {
        return vedtakId;
    }

    public void setVedtakId(String vedtakId) {
        this.vedtakId = vedtakId;
    }

    public String getUtbetalesTilId() {
        return utbetalesTilId;
    }

    public void setUtbetalesTilId(String utbetalesTilId) {
        this.utbetalesTilId = utbetalesTilId;
    }

    public Long getDelytelseId() {
        return delytelseId;
    }

    public void setDelytelseId(Long delytelseId) {
        this.delytelseId = delytelseId;
    }

    public Long getRefDelytelseId() {
        return refDelytelseId;
    }

    public void setRefDelytelseId(Long refDelytelseId) {
        this.refDelytelseId = refDelytelseId;
    }

    public Long getRefFagsystemId() {
        return refFagsystemId;
    }

    public void setRefFagsystemId(Long refFagsystemId) {
        this.refFagsystemId = refFagsystemId;
    }

    public String getKodeEndringLinje() {
        return kodeEndringLinje;
    }

    public void setKodeEndringLinje(String kodeEndringLinje) {
        this.kodeEndringLinje = kodeEndringLinje;
    }

    public String getKodeStatusLinje() {
        return kodeStatusLinje;
    }

    public void setKodeStatusLinje(String kodeStatusLinje) {
        this.kodeStatusLinje = kodeStatusLinje;
    }

    public LocalDate getDatoStatusFom() {
        return datoStatusFom;
    }

    public void setDatoStatusFom(LocalDate datoStatusFom) {
        this.datoStatusFom = datoStatusFom;
    }

    public LocalDate getDatoVedtakFom() {
        return datoVedtakFom;
    }

    public void setDatoVedtakFom(LocalDate datoVedtakFom) {
        this.datoVedtakFom = datoVedtakFom;
    }

    public LocalDate getDatoVedtakTom() {
        return datoVedtakTom;
    }

    public void setDatoVedtakTom(LocalDate datoVedtakTom) {
        this.datoVedtakTom = datoVedtakTom;
    }

    public KodeKlassifik getKodeKlassifik() {
        return kodeKlassifik;
    }

    public void setKodeKlassifik(KodeKlassifik kodeKlassifik) {
        this.kodeKlassifik = kodeKlassifik;
    }

    public long getSats() {
        return sats;
    }

    public void setSats(long sats) {
        this.sats = sats;
    }

    public String getTypeSats() {
        return typeSats;
    }

    public void setTypeSats(String typeSats) {
        this.typeSats = typeSats;
    }

    public RefusjonInfo156Dto getRefusjonInfo156() {
        return refusjonInfo156;
    }

    public Integer getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public void setUtbetalingsgrad(Integer utbetalingsgrad) {
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public void setRefusjonInfo156(RefusjonInfo156Dto refusjonInfo156) {
        this.refusjonInfo156 = refusjonInfo156;
    }

    public static Oppdragslinje150Dto fraDomene(Oppdragslinje150 oppdragslinje150) {
        var oppdragslinje150Dto = new Oppdragslinje150Dto(oppdragslinje150);
        oppdragslinje150Dto.vedtakId = oppdragslinje150.getVedtakId();
        oppdragslinje150Dto.utbetalesTilId = oppdragslinje150.getUtbetalesTilId();
        oppdragslinje150Dto.delytelseId = oppdragslinje150.getDelytelseId();
        oppdragslinje150Dto.refDelytelseId = oppdragslinje150.getRefDelytelseId();
        oppdragslinje150Dto.refFagsystemId = oppdragslinje150.getRefFagsystemId();
        oppdragslinje150Dto.kodeEndringLinje = oppdragslinje150.getKodeEndringLinje().name();
        oppdragslinje150Dto.kodeStatusLinje = oppdragslinje150.getKodeStatusLinje().name();
        oppdragslinje150Dto.datoStatusFom = oppdragslinje150.getDatoStatusFom();
        oppdragslinje150Dto.datoVedtakFom = oppdragslinje150.getDatoVedtakFom();
        oppdragslinje150Dto.datoVedtakTom = oppdragslinje150.getDatoVedtakTom();
        oppdragslinje150Dto.kodeKlassifik = oppdragslinje150.getKodeKlassifik();
        oppdragslinje150Dto.sats = oppdragslinje150.getSats().getVerdi().longValue();
        oppdragslinje150Dto.typeSats = oppdragslinje150.getTypeSats().name();
        oppdragslinje150Dto.utbetalingsgrad = oppdragslinje150.getUtbetalingsgrad().getVerdi();
        oppdragslinje150Dto.refusjonInfo156 = oppdragslinje150.getRefusjonsinfo156() != null ? RefusjonInfo156Dto.fraDomene(oppdragslinje150.getRefusjonsinfo156()) : null;
        return oppdragslinje150Dto;

    }
}
