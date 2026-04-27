-- 35 tutorial Excel awal — status default 'draft', menunggu admin trigger generate.
-- INSERT OR IGNORE: aman dijalankan ulang, tidak menimpa data yang sudah ada.

INSERT OR IGNORE INTO excels (id, title, category) VALUES ('vlookup-dasar',              'VLOOKUP Dasar untuk Pemula',                    'Lookup');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('vlookup-multi-sheet',        'VLOOKUP Antar Sheet untuk Rekap Karyawan',      'Lookup');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('xlookup-pengganti-vlookup',  'XLOOKUP: Pengganti VLOOKUP yang Lebih Fleksibel','Lookup');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('index-match-kombo',          'INDEX MATCH: Kombinasi Lookup Profesional',     'Lookup');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('sumif-rekap-omzet',          'SUMIF untuk Rekap Omzet Warung per Hari',       'Function');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('sumifs-multi-kriteria',      'SUMIFS dengan Beberapa Kriteria',               'Function');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('countif-absensi',            'COUNTIF untuk Hitung Kehadiran Karyawan',       'Function');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('if-bersarang',               'IF Bersarang untuk Klasifikasi Nilai Siswa',    'Function');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('ifs-pengganti-if',           'IFS: Cara Lebih Rapi dari IF Bersarang',        'Function');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('iferror-handle-na',          'IFERROR: Handle Error #N/A di VLOOKUP',         'Function');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('concatenate-textjoin',       'TEXTJOIN: Gabung Banyak Sel dengan Pemisah',    'Function');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('left-right-mid',             'LEFT, RIGHT, MID: Ambil Bagian Teks',           'Function');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('datedif-hitung-umur',        'DATEDIF: Hitung Umur atau Masa Kerja',          'Function');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('today-now',                  'TODAY dan NOW untuk Tanggal Otomatis',          'Function');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('pivot-table-dasar',          'PivotTable Dasar untuk Rekap Penjualan',        'PivotTable');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('pivot-table-grouping',       'Grouping di PivotTable: Per Bulan, Per Wilayah','PivotTable');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('pivot-calculated-field',     'Calculated Field di PivotTable',                'PivotTable');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('slicer-pivot',               'Slicer untuk Filter PivotTable Interaktif',     'PivotTable');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('chart-bar-vs-column',        'Bar Chart vs Column Chart: Kapan Pakai Mana',   'Chart');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('chart-pie-donut',            'Pie Chart untuk Komposisi Penjualan',           'Chart');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('chart-combo',                'Combo Chart: Kombinasi Bar dan Line',           'Chart');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('sparkline-mini-grafik',      'Sparkline: Mini Grafik di Satu Sel',            'Chart');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('conditional-formatting-warna','Conditional Formatting Warna untuk Stok',      'Format');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('data-bars-icon-set',         'Data Bars dan Icon Set untuk Visualisasi Cepat','Format');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('freeze-panes',               'Freeze Panes: Kunci Header saat Scroll',        'Format');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('format-rupiah',              'Format Cell Rupiah dengan Pemisah Ribuan',      'Format');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('filter-dasar',               'Filter Data Tabel: Cara Cepat Cari Baris',      'Database');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('advanced-filter',            'Advanced Filter: Multi Kriteria Kompleks',      'Database');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('remove-duplicates',          'Hapus Data Duplikat dalam Sekali Klik',         'Database');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('data-validation-dropdown',   'Data Validation: Buat Dropdown List',           'Database');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('macro-rekam-dasar',          'Rekam Macro Pertama untuk Otomasi Sederhana',   'Macro');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('vba-loop-baris',             'VBA: Loop untuk Proses Banyak Baris Otomatis',  'Macro');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('sum-average-dasar',          'SUM, AVERAGE, MIN, MAX untuk Pemula',           'BasicFormula');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('absolute-relative-reference','Perbedaan $A$1 vs A1: Absolute vs Relative',    'BasicFormula');
INSERT OR IGNORE INTO excels (id, title, category) VALUES ('autofill-fill-handle',       'AutoFill: Trik Cepat Isi Data Berurutan',       'BasicFormula');
