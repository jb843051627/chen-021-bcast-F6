-- bcast 广播电视安全播出与播出事故处置管理 -- schema (chen-021)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_021

CREATE TABLE IF NOT EXISTS t_bcast_case_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '播出事故处置单代号',
  stage int DEFAULT NULL COMMENT '当前步次 0..3（发现核实/处置抢险/恢复确认/上报结案）',
  status int DEFAULT NULL COMMENT '处置会落 0未起 1在办 2已收了口',
  content varchar(2000) DEFAULT NULL COMMENT '一段一记（挪步出口落下的分段记载：第k步|时刻|话；沉底的、收口的也留在这）',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次挪步动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='播出事故处置单';

CREATE TABLE IF NOT EXISTS t_bcast_off_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '运行情况报送册批号',
  row_no int DEFAULT NULL COMMENT '原报行次',
  item_code varchar(64) DEFAULT NULL COMMENT '被对上的监测台代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行停播分钟数',
  hours_num decimal(12,2) DEFAULT NULL COMMENT '本季满播小时数',
  status int DEFAULT NULL COMMENT '行落地情形 0未销 1已收下 2已退回',
  arc_flag int DEFAULT '0' COMMENT '挪出标记 0现行 1已挪出(连着两个报送季未销，挪出现行名册；不是抹掉，老册照翻)',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运行情况报送核收行';

CREATE TABLE IF NOT EXISTS t_bcast_pre_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '定档门槛线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  line_kind int DEFAULT '1' COMMENT '线管哪一头 1本段停播分钟数 2被影响到的户数',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '起算数值(分钟/户)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '过半数值(分钟/户)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '封顶数值(分钟/户)',
  eff_start datetime DEFAULT NULL COMMENT '立线之日',
  eff_end datetime DEFAULT NULL COMMENT '让位之日(不含；留白按一直在场认——旧卷存量靠它复算)',
  priority int DEFAULT NULL COMMENT '让位顺位(数值大的先说话)',
  status int DEFAULT NULL COMMENT '线的情形 0在场 1已停用(退居册底，翻得到、不进现行判定)',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='事故定档门槛线';

CREATE TABLE IF NOT EXISTS t_bcast_site (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '发射台站代号',
  site_name varchar(128) DEFAULT NULL COMMENT '台站全称',
  site_type varchar(32) DEFAULT NULL COMMENT '台站类别',
  road_name varchar(128) DEFAULT NULL COMMENT '属地那一路(省—市—县)',
  status int DEFAULT NULL COMMENT '底册情形 0在册 1已关停',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发射台站底册';

CREATE TABLE IF NOT EXISTS t_bcast_stop_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '停播检修核准单',
  node_no int DEFAULT NULL COMMENT '当前所在档口 0..2（台站/市广电局/省局）',
  sign_mode int DEFAULT NULL COMMENT '同档点齐办法 0一笔即可 1两笔点齐',
  need_count int DEFAULT NULL COMMENT '本档应点名数',
  sign_count int DEFAULT NULL COMMENT '本档已落名数',
  status int DEFAULT NULL COMMENT '核准情形 0在核 1已排定 2已否决',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='停播检修核准单';

CREATE TABLE IF NOT EXISTS t_bcast_stop_sign (
  id bigint NOT NULL COMMENT '主键',
  bill_id bigint DEFAULT NULL COMMENT '所属核准单主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '停播检修核准单代号',
  node_no int DEFAULT NULL COMMENT '落在哪一档 0..2（台站/市广电局/省局）',
  round_no int DEFAULT NULL COMMENT '这一档走到第几轮（退一回加一轮，头一轮为1）',
  seq_no int DEFAULT NULL COMMENT '档内名次：这一档这一轮里的第几名',
  signer varchar(64) DEFAULT NULL COMMENT '落名人',
  comment varchar(500) DEFAULT NULL COMMENT '落名时记的话',
  kind int DEFAULT '0' COMMENT '落名种类 0点齐落名 1否决落名',
  alive_flag int DEFAULT '0' COMMENT '行的情形 0在册作数 1沉底不再作数',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '落名时刻',
  PRIMARY KEY (id),
  KEY idx_bcast_stop_sign_bill (bill_id, node_no, round_no, alive_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='停播检修核准单落名底账';

CREATE TABLE IF NOT EXISTS t_bcast_stop_schedule (
  id bigint NOT NULL COMMENT '主键',
  bill_id bigint DEFAULT NULL COMMENT '所属核准单主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '停播检修核准单代号',
  node_no int DEFAULT NULL COMMENT '这一笔原落在哪一档 0..2（台站/市广电局/省局）',
  round_no int DEFAULT NULL COMMENT '这一档排定时作数的轮次',
  seq_no int DEFAULT NULL COMMENT '档内名次',
  signer varchar(64) DEFAULT NULL COMMENT '落名人',
  sign_id bigint DEFAULT NULL COMMENT '所对的落名底账行主键',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '誊入名册者',
  create_time datetime DEFAULT NULL COMMENT '誊入名册时刻',
  PRIMARY KEY (id),
  KEY idx_bcast_stop_schedule_bill (bill_id, node_no, seq_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='停播检修排期名册';

CREATE TABLE IF NOT EXISTS t_bcast_unit_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '台站核点卡卡号',
  node_no int DEFAULT NULL COMMENT '属地层级序',
  site_id int DEFAULT NULL COMMENT '所属发射台站',
  site_no varchar(64) DEFAULT NULL COMMENT '所属台站代号',
  should_count int DEFAULT NULL COMMENT '应核点位数',
  done_count int DEFAULT NULL COMMENT '已核点位数',
  lack_count int DEFAULT NULL COMMENT '还欠点位数',
  content varchar(255) DEFAULT NULL COMMENT '随卡交来的属地要件',
  status int DEFAULT NULL COMMENT '立卡进展 0待核 1已核讫 2压卡',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='台站核点卡立卡单';

CREATE TABLE IF NOT EXISTS t_bcast_zero_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '零报告催办条目代号',
  due_at datetime DEFAULT NULL COMMENT '该出手那一日的止点时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '最可提前几日开口',
  content varchar(255) DEFAULT NULL COMMENT '事由与所对台站的记要',
  status int DEFAULT NULL COMMENT '条目情形 0候催 1已催到 2催不动',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='零报告催办到期单';

CREATE TABLE IF NOT EXISTS t_bcast_case_grade (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '事故处置单代号',
  occur_at datetime DEFAULT NULL COMMENT '事故回看那一刻(归层就按这一刻查线，补算往期也用它自己那一刻)',
  stop_minutes decimal(12,2) DEFAULT NULL COMMENT '本段停播分钟数(分钟那头的数)',
  affect_households decimal(12,2) DEFAULT NULL COMMENT '被影响到的户数(户数那头的数)',
  grade int DEFAULT NULL COMMENT '归到第几层 1..3；没够上(一头无线/一头越封顶)时留白',
  settled int DEFAULT '0' COMMENT '这笔落没落定 0没够上 1归层落定(没够上与后来补上各是一笔，不回改)',
  minute_rule_code varchar(64) DEFAULT NULL COMMENT '落库这一趟分钟线代号(钉死，线册日后换法不回头改这一笔)',
  minute_eff_start datetime DEFAULT NULL COMMENT '分钟线立线之日(钉死)',
  house_rule_code varchar(64) DEFAULT NULL COMMENT '落库这一趟户数线代号(钉死)',
  house_eff_start datetime DEFAULT NULL COMMENT '户数线立线之日(钉死)',
  exceed_note varchar(500) DEFAULT NULL COMMENT '越限摆数：哪一头的数高过了自己那条线的封顶，把两头的数摆出来',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_bcast_case_grade_biz (biz_no, del_flag),
  KEY idx_bcast_case_grade_occur (occur_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='事故定档结论(每趟一笔，钉住当趟的线)';

-- 台站底册初始数据：id=0 在册启用（status=0）、id=1 已关停（status=1）
INSERT IGNORE INTO t_bcast_site (id, site_no, site_name, site_type, road_name, status, del_flag, create_by, create_time)
VALUES (0, 'TZ00', '苗岭转播台（无线发射，在册）', '无线发射台', '长岭省—河湾区—苗岭县', 0, 0, 'seed', NOW()),
       (1, 'TZ01', '滨河北微波站（线路迁改后关停）', '微波站', '长岭省—河湾区—滨河北', 1, 0, 'seed', NOW());

-- 门槛线册初始数据。省局说法改过三轮，每轮新添一条、旧的填上让位之日退居册底：
-- 到期让位（eff_end 有值、status 仍为在场）与整条停用（status=1）是两回事——
-- 让位线在[立线,让位)区间内永远作数，旧卷结论靠它复算；停用线任何一刻都不进判定。
-- 分钟那头三轮（line_kind=1）：
INSERT IGNORE INTO t_bcast_pre_line
(id, rule_code, rule_name, line_kind, th1_max, th2_max, th3_max, eff_start, eff_end, priority, status, del_flag, create_by, create_time, remark)
VALUES (101, 'PL-F01', '二〇二三老说法（分钟）', 1, 10.00, 30.00, 60.00,
        '2023-01-01 00:00:00', '2024-01-01 00:00:00', 10, 0, 0, 'seed', NOW(),
        '第一轮说法，二〇二四说法立线当日起让位；区间内旧结论照它复算'),
       (102, 'PL-F02', '二〇二四说法（分钟）', 1, 5.00, 20.00, 45.00,
        '2024-01-01 00:00:00', '2025-06-01 00:00:00', 20, 0, 0, 'seed', NOW(),
        '第二轮说法，交接当日只管到当日之前；现行说法当日进场'),
       (103, 'PL-F03', '现行说法（分钟）', 1, 3.00, 15.00, 30.00,
        '2025-06-01 00:00:00', NULL, 30, 0, 0, 'seed', NOW(),
        '第三轮说法，让位栏留白，按一直在场认');
-- 户数那头两轮（line_kind=2）：老说法整条停用、新说法去年九月才立——
-- 去年那两份照着停用线 PL-H01 定下来的处置，在事故那一日找不出在场户数线，历不对。
INSERT IGNORE INTO t_bcast_pre_line
(id, rule_code, rule_name, line_kind, th1_max, th2_max, th3_max, eff_start, eff_end, priority, status, del_flag, create_by, create_time, remark)
VALUES (201, 'PL-H01', '户数老说法（已整条停用）', 2, 500.00, 2000.00, 5000.00,
        '2024-01-01 00:00:00', NULL, 10, 1, 0, 'seed', NOW(),
        '立错撤下的线：让位栏虽留白，已停用就任何一刻都不作数，屏上翻得到、不许再选'),
       (202, 'PL-H02', '户数现行说法', 2, 300.00, 1000.00, 3000.00,
        '2025-09-01 00:00:00', NULL, 20, 0, 0, 'seed', NOW(),
        '去年九月才立，之前的事故靠不着它');

-- 定档结论初始数据：前两份挂着已停用的 PL-H01，事故那一日户数那头没有在场线——历不对；
-- 第三份两头都对得上当刻在场线，是落定的结论，历不对清点不许带上它。
INSERT IGNORE INTO t_bcast_case_grade
(id, biz_no, occur_at, stop_minutes, affect_households, grade, settled,
 minute_rule_code, minute_eff_start, house_rule_code, house_eff_start, exceed_note,
 del_flag, create_by, create_time, remark)
VALUES (901, 'SG-2025-001', '2025-03-10 09:20:00', 12.00, 800.00, 1, 1,
        'PL-F02', '2024-01-01 00:00:00', 'PL-H01', '2024-01-01 00:00:00', NULL,
        0, 'seed', '2025-03-10 10:00:00', '去年照着已停用户数线定的，历不对'),
       (902, 'SG-2025-002', '2025-04-02 14:00:00', 30.00, 2600.00, 2, 1,
        'PL-F02', '2024-01-01 00:00:00', 'PL-H01', '2024-01-01 00:00:00', NULL,
        0, 'seed', '2025-04-02 15:00:00', '去年照着已停用户数线定的，历不对'),
       (903, 'SG-2026-001', '2026-03-15 08:30:00', 10.00, 500.00, 1, 1,
        'PL-F03', '2025-06-01 00:00:00', 'PL-H02', '2025-09-01 00:00:00', NULL,
        0, 'seed', '2026-03-15 09:00:00', '两头都对得上当刻在场线，落定');

