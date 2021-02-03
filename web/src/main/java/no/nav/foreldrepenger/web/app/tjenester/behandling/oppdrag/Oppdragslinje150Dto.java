package no.nav.foreldrepenger.web.app.tjenester.behandling.oppdrag;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;

public class Oppdragslinje150Dto extends SporingDto {

    private String vedtakId;
    private String saksbehId;
    private String utbetalesTilId;
    private Long delytelseId;
    private Long refDelytelseId;
    private Long refFagsystemId;
    private String kodeEndringLinje;
    private String kodeStatusLinje;
    private LocalDate datoStatusFom;
    private LocalDate datoVedtakFom;
    private LocalDate datoVedtakTom;
    private String kodeKlassifik;
    private long sats;
    private String typeSats;
    private String fradragTillegg;
    private String brukKjoreplan;
    private Long henvisning;
    private RefusjonInfo156Dto refusjonInfo156;
    private List<Grad170Dto> grad170;

    public Oppdragslinje150Dto(Oppdragslinje150 entitet) {
        super(entitet, entitet.getVersjon(), entitet.getId());
    }

    public String getVedtakId() {
        return vedtakId;
    }

    public void setVedtakId(String vedtakId) {
        this.vedtakId = vedtakId;
    }

    public String getSaksbehId() {
        return saksbehId;
    }

    public void setSaksbehId(String saksbehId) {
        this.saksbehId = saksbehId;
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

    public String getKodeKlassifik() {
        return kodeKlassifik;
    }

    public void setKodeKlassifik(String kodeKlassifik) {
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

    public String getFradragTillegg() {
        return fradragTillegg;
    }

    public void setFradragTillegg(String fradragTillegg) {
        this.fradragTillegg = fradragTillegg;
    }

    public String getBrukKjoreplan() {
        return brukKjoreplan;
    }

    public void setBrukKjoreplan(String brukKjoreplan) {
        this.brukKjoreplan = brukKjoreplan;
    }

    public Long getHenvisning() {
        return henvisning;
    }

    public void setHenvisning(Long henvisning) {
        this.henvisning = henvisning;
    }

    public RefusjonInfo156Dto getRefusjonInfo156() {
        return refusjonInfo156;
    }

    public void setRefusjonInfo156(RefusjonInfo156Dto refusjonInfo156) {
        this.refusjonInfo156 = refusjonInfo156;
    }

    public List<Grad170Dto> getGrad170() {
        return grad170;
    }

    public void setGrad170(List<Grad170Dto> grad170) {
        this.grad170 = grad170;
    }

    public static Oppdragslinje150Dto fraDomene(Oppdragslinje150 oppdragslinje150) {
        Oppdragslinje150Dto oppdragslinje150Dto = new Oppdragslinje150Dto(oppdragslinje150);
        oppdragslinje150Dto.vedtakId = oppdragslinje150.getVedtakId();
        oppdragslinje150Dto.saksbehId = oppdragslinje150.getSaksbehId();
        oppdragslinje150Dto.utbetalesTilId = oppdragslinje150.getUtbetalesTilId();
        oppdragslinje150Dto.delytelseId = oppdragslinje150.getDelytelseId();
        oppdragslinje150Dto.refDelytelseId = oppdragslinje150.getRefDelytelseId();
        oppdragslinje150Dto.refFagsystemId = oppdragslinje150.getRefFagsystemId();
        oppdragslinje150Dto.kodeEndringLinje = oppdragslinje150.getKodeEndringLinje();
        oppdragslinje150Dto.kodeStatusLinje = oppdragslinje150.getKodeStatusLinje();
        oppdragslinje150Dto.datoStatusFom = oppdragslinje150.getDatoStatusFom();
        oppdragslinje150Dto.datoVedtakFom = oppdragslinje150.getDatoVedtakFom();
        oppdragslinje150Dto.datoVedtakTom = oppdragslinje150.getDatoVedtakTom();
        oppdragslinje150Dto.kodeKlassifik = oppdragslinje150.getKodeKlassifik();
        oppdragslinje150Dto.sats = oppdragslinje150.getSats();
        oppdragslinje150Dto.typeSats = oppdragslinje150.getTypeSats();
        oppdragslinje150Dto.fradragTillegg = oppdragslinje150.getFradragTillegg();
        oppdragslinje150Dto.brukKjoreplan = oppdragslinje150.getBrukKjoreplan();
        oppdragslinje150Dto.henvisning = oppdragslinje150.getHenvisning();
        oppdragslinje150Dto.refusjonInfo156 = oppdragslinje150.getRefusjonsinfo156() != null ? RefusjonInfo156Dto.fraDomene(oppdragslinje150.getRefusjonsinfo156()) : null;
        oppdragslinje150Dto.grad170 = oppdragslinje150.getGrad170Liste()
            .stream()
            .map(Grad170Dto::fraDomene)
            .collect(Collectors.toList());
        return oppdragslinje150Dto;

    }
}
